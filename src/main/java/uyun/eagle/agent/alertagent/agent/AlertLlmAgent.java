package uyun.eagle.agent.alertagent.agent;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uyun.eagle.agent.alertagent.config.LlmProperties;
import uyun.eagle.agent.alertagent.knowledge.KnowledgeBaseLoader;
import uyun.eagle.agent.alertagent.llm.LlmChatClient;
import uyun.eagle.agent.alertagent.mcp.McpToolRegistry;
import uyun.eagle.agent.alertagent.agent.dto.AgentChatResponse;
/**
 * 基于 LLM 的告警对话编排（Phase 1b）。
 *
 * <p>用 OpenAI 兼容的 Tool Calling 流程：把 {@link McpToolRegistry} 的 4 个只读查询工具
 * 作为 functions 暴露给 LLM，由 LLM 理解意图、选择并填参调用，再基于工具返回的真实数据组织回复/研判。
 *
 * <p>工具定义与执行都复用 {@link McpToolRegistry}（与 MCP 同一套），不重复定义业务逻辑；
 * 全程只读，不涉及任何写操作。
 */
@Slf4j
@Service
public class AlertLlmAgent {

    private static final Gson GSON = new Gson();

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private LlmChatClient llmChatClient;

    @Autowired
    private McpToolRegistry toolRegistry;

    @Autowired
    private KnowledgeBaseLoader knowledgeBaseLoader;

    /**
     * 以 LLM + Tool Calling 处理一次对话。
     *
     * @param message   用户消息（非空）
     * @param sessionId 会话 ID（透传回显）
     * @return 对话响应，intent 固定为 llm
     * @throws RuntimeException LLM 调用异常时抛出，交由上层降级处理
     */
    public AgentChatResponse chat(String message, String sessionId) {
        JsonArray messages = new JsonArray();
        messages.add(textMessage("system", buildSystemPrompt()));
        messages.add(textMessage("user", message));

        JsonArray tools = buildTools();
        int maxRounds = Math.max(1, llmProperties.getMaxToolRounds());

        for (int round = 0; round < maxRounds; round++) {
            JsonObject body = new JsonObject();
            body.addProperty("model", llmProperties.getModel());
            body.addProperty("temperature", llmProperties.getTemperature());
            body.add("messages", messages);
            body.add("tools", tools);
            body.addProperty("tool_choice", "auto");

            JsonObject rsp = llmChatClient.chatCompletion(body);
            JsonObject assistant = firstMessage(rsp);
            if (assistant == null) {
                throw new LlmChatClient.LlmException("LLM 响应缺少 choices.message");
            }
            // 助手消息需原样回填到上下文（含 tool_calls），否则下一轮工具结果无法对应
            messages.add(assistant);

            JsonArray toolCalls = optArray(assistant, "tool_calls");
            if (toolCalls == null || toolCalls.size() == 0) {
                String content = optString(assistant, "content");
                String reply = (content == null || content.trim().isEmpty())
                        ? "未能生成回复，请换个问法试试。" : content.trim();
                return new AgentChatResponse(reply, sessionId, "llm");
            }

            // 执行本轮所有工具调用，把结果作为 role=tool 消息回填
            for (JsonElement el : toolCalls) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject call = el.getAsJsonObject();
                messages.add(executeToolCall(call));
            }
        }

        log.warn("[AlertLlmAgent] 达到最大工具轮数 {}，仍未产出最终回复", maxRounds);
        return new AgentChatResponse("查询轮次过多，请缩小问题范围后重试。", sessionId, "llm");
    }

    /**
     * 构造 system 提示：基础角色约束 + （可选）运维知识库。
     * 知识库为空时退化为仅基础约束，不影响对话。
     */
    private String buildSystemPrompt() {
        String knowledge = knowledgeBaseLoader.getKnowledgeText();
        if (knowledge == null || knowledge.trim().isEmpty()) {
            return AlertAgentPrompts.SYSTEM_PROMPT;
        }
        return AlertAgentPrompts.SYSTEM_PROMPT
                + "\n\n" + AlertAgentPrompts.KNOWLEDGE_PREAMBLE
                + "\n" + knowledge.trim()
                + "\n" + AlertAgentPrompts.KNOWLEDGE_SUFFIX;
    }

    /** 执行单个 tool_call，返回对应的 role=tool 消息。工具异常以文本回传给 LLM，不中断对话。 */
    private JsonObject executeToolCall(JsonObject call) {
        String callId = optString(call, "id");
        String name = null;
        String content;
        try {
            JsonObject function = call.has("function") && call.get("function").isJsonObject()
                    ? call.getAsJsonObject("function") : null;
            if (function == null) {
                throw new IllegalArgumentException("tool_call 缺少 function");
            }
            name = optString(function, "name");
            JsonObject args = parseArguments(optString(function, "arguments"));
            Object result = toolRegistry.callTool(name, args);
            content = GSON.toJson(result);
        } catch (Exception e) {
            log.warn("[AlertLlmAgent] 工具 {} 执行失败: {}", name, e.getMessage());
            content = "工具执行失败：" + e.getMessage();
        }

        JsonObject toolMsg = new JsonObject();
        toolMsg.addProperty("role", "tool");
        if (callId != null) {
            toolMsg.addProperty("tool_call_id", callId);
        }
        if (name != null) {
            toolMsg.addProperty("name", name);
        }
        toolMsg.addProperty("content", content);
        return toolMsg;
    }

    /** 把 MCP 工具清单转换为 OpenAI tools（function）格式。 */
    private JsonArray buildTools() {
        JsonArray tools = new JsonArray();
        for (JsonElement el : toolRegistry.listTools()) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject mcpTool = el.getAsJsonObject();
            JsonObject function = new JsonObject();
            function.addProperty("name", optString(mcpTool, "name"));
            function.addProperty("description", optString(mcpTool, "description"));
            if (mcpTool.has("inputSchema") && mcpTool.get("inputSchema").isJsonObject()) {
                function.add("parameters", mcpTool.getAsJsonObject("inputSchema"));
            }
            JsonObject tool = new JsonObject();
            tool.addProperty("type", "function");
            tool.add("function", function);
            tools.add(tool);
        }
        return tools;
    }

    private static JsonObject parseArguments(String arguments) {
        if (arguments == null || arguments.trim().isEmpty()) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(arguments);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static JsonObject firstMessage(JsonObject rsp) {
        if (rsp == null || !rsp.has("choices") || !rsp.get("choices").isJsonArray()) {
            return null;
        }
        JsonArray choices = rsp.getAsJsonArray("choices");
        if (choices.size() == 0 || !choices.get(0).isJsonObject()) {
            return null;
        }
        JsonObject choice = choices.get(0).getAsJsonObject();
        return choice.has("message") && choice.get("message").isJsonObject()
                ? choice.getAsJsonObject("message") : null;
    }

    private static JsonObject textMessage(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private static JsonArray optArray(JsonObject o, String key) {
        return (o != null && o.has(key) && o.get(key).isJsonArray()) ? o.getAsJsonArray(key) : null;
    }

    private static String optString(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
