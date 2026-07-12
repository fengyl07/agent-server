package uyun.eagle.agent.alertagent.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MCP JSON-RPC 2.0 协议处理（Phase 1）。
 *
 * <p>支持方法：
 * <ul>
 *     <li>{@code initialize} —— 握手，返回 protocolVersion / capabilities / serverInfo</li>
 *     <li>{@code notifications/initialized} —— 通知，无响应</li>
 *     <li>{@code ping} —— 返回空结果</li>
 *     <li>{@code tools/list} —— 返回工具清单</li>
 *     <li>{@code tools/call} —— 调用工具，结果以 text content 返回</li>
 * </ul>
 * 工具自身抛出的业务异常通过 result.isError=true 返回（便于客户端/LLM 读取），
 * 协议级错误（方法不存在、参数非法）使用 JSON-RPC error 返回。
 */
@Slf4j
@Component
public class McpProtocolHandler {

    private static final Gson GSON = new Gson();

    private static final int ERR_INVALID_REQUEST = -32600;
    private static final int ERR_METHOD_NOT_FOUND = -32601;
    private static final int ERR_INVALID_PARAMS = -32602;
    private static final int ERR_INTERNAL = -32603;

    @Autowired
    private McpProperties mcpProperties;

    @Autowired
    private McpToolRegistry toolRegistry;

    /**
     * 处理单个 JSON-RPC 消息。
     *
     * @param request 已解析的请求对象
     * @return 响应对象；若是通知（无 id 或 initialized 通知），返回 null 表示无需回复
     */
    public JsonObject handle(JsonObject request) {
        if (request == null || !request.has("method") || request.get("method").isJsonNull()) {
            return error(idOf(request), ERR_INVALID_REQUEST, "无效请求：缺少 method");
        }
        String method = request.get("method").getAsString();
        JsonElement id = idOf(request);
        boolean isNotification = id == null;

        try {
            switch (method) {
                case "initialize":
                    return success(id, initialize(paramsOf(request)));
                case "notifications/initialized":
                case "initialized":
                    return null;
                case "ping":
                    return success(id, new JsonObject());
                case "tools/list":
                    return success(id, toolsList());
                case "tools/call":
                    return success(id, toolsCall(paramsOf(request)));
                default:
                    if (isNotification) {
                        return null;
                    }
                    return error(id, ERR_METHOD_NOT_FOUND, "不支持的方法：" + method);
            }
        } catch (IllegalArgumentException e) {
            return error(id, ERR_INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            log.error("[MCP] 处理方法 {} 失败: {}", method, e.getMessage(), e);
            return error(id, ERR_INTERNAL, "服务器内部错误：" + e.getMessage());
        }
    }

    private JsonObject initialize(JsonObject params) {
        String protocolVersion = mcpProperties.getProtocolVersion();
        if (params != null && params.has("protocolVersion") && !params.get("protocolVersion").isJsonNull()) {
            // 回显客户端声明的协议版本，提升兼容性
            protocolVersion = params.get("protocolVersion").getAsString();
        }

        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", protocolVersion);

        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", mcpProperties.getServerName());
        serverInfo.addProperty("version", mcpProperties.getServerVersion());
        result.add("serverInfo", serverInfo);

        return result;
    }

    private JsonObject toolsList() {
        JsonObject result = new JsonObject();
        result.add("tools", toolRegistry.listTools());
        return result;
    }

    private JsonObject toolsCall(JsonObject params) {
        if (params == null || !params.has("name") || params.get("name").isJsonNull()) {
            throw new IllegalArgumentException("tools/call 缺少 name");
        }
        String name = params.get("name").getAsString();
        JsonObject arguments = null;
        if (params.has("arguments") && params.get("arguments").isJsonObject()) {
            arguments = params.getAsJsonObject("arguments");
        }

        try {
            Object toolResult = toolRegistry.callReadOnlyTool(name, arguments);
            return toolResultContent(GSON.toJson(toolResult), false);
        } catch (IllegalArgumentException e) {
            // 参数类问题归为协议级错误，向上抛由 handle 转 JSON-RPC error
            throw e;
        } catch (Exception e) {
            log.error("[MCP] 工具 {} 执行失败: {}", name, e.getMessage(), e);
            // 工具执行期异常作为 isError 结果返回，便于客户端展示
            return toolResultContent("工具执行失败：" + e.getMessage(), true);
        }
    }

    private JsonObject toolResultContent(String text, boolean isError) {
        JsonObject result = new JsonObject();
        JsonArray content = new JsonArray();
        JsonObject block = new JsonObject();
        block.addProperty("type", "text");
        block.addProperty("text", text);
        content.add(block);
        result.add("content", content);
        result.addProperty("isError", isError);
        return result;
    }

    private static JsonElement idOf(JsonObject request) {
        if (request != null && request.has("id") && !request.get("id").isJsonNull()) {
            return request.get("id");
        }
        return null;
    }

    private static JsonObject paramsOf(JsonObject request) {
        if (request.has("params") && request.get("params").isJsonObject()) {
            return request.getAsJsonObject("params");
        }
        return null;
    }

    private static JsonObject success(JsonElement id, JsonElement result) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.add("id", id == null ? JsonNull.INSTANCE : id);
        o.add("result", result);
        return o;
    }

    private static JsonObject error(JsonElement id, int code, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.add("id", id == null ? JsonNull.INSTANCE : id);
        JsonObject err = new JsonObject();
        err.addProperty("code", code);
        err.addProperty("message", message == null ? "" : message);
        o.add("error", err);
        return o;
    }
}
