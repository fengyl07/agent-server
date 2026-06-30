package uyun.eagle.agent.alertagent.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.config.RagProperties;
import uyun.eagle.agent.alertagent.tool.AlertQueryTools;
import uyun.eagle.agent.alertagent.tool.KnowledgeSearchTools;

/**
 * MCP 工具注册表（Phase 1，只读）。
 *
 * <p>把已有的 {@link AlertQueryTools} 4 个查询能力包装成 MCP Tool：
 * 提供 {@code tools/list} 所需的 name/description/inputSchema，
 * 以及 {@code tools/call} 的参数解析与调用分发。
 *
 * <p>不在此处新增任何业务逻辑，只做「MCP 协议 ↔ 现有 Tool」的适配。
 */
@Slf4j
@Component
public class McpToolRegistry {

    static final String TOOL_COUNT_TODAY = "count_today_alerts";
    static final String TOOL_QUERY = "query_alerts";
    static final String TOOL_DETAIL = "get_alert_detail";
    static final String TOOL_SIMILAR = "find_similar_alerts";
    static final String TOOL_SEARCH_KNOWLEDGE = "search_knowledge";

    private static final Gson GSON = new Gson();

    @Autowired
    private AlertQueryTools alertQueryTools;

    @Autowired
    private KnowledgeSearchTools knowledgeSearchTools;

    @Autowired
    private RagProperties ragProperties;

    /**
     * 返回 {@code tools/list} 的工具清单。
     */
    public JsonArray listTools() {
        JsonArray tools = new JsonArray();

        tools.add(tool(TOOL_COUNT_TODAY,
                "统计今天的告警数量。可选按状态、级别过滤。",
                "{"
                        + "\"type\":\"object\","
                        + "\"properties\":{"
                        + "\"status\":{\"type\":\"string\",\"description\":\"状态码：0未接手/40已确认/150处理中/190已解决/255已关闭，可空\"},"
                        + "\"severity\":{\"type\":\"string\",\"description\":\"级别码：3紧急/2错误/1警告/0恢复，可空\"}"
                        + "}}"));

        tools.add(tool(TOOL_QUERY,
                "查询告警列表。默认只返回未关闭告警，按最后发生时间倒序。支持按级别、状态、对象名过滤。",
                "{"
                        + "\"type\":\"object\","
                        + "\"properties\":{"
                        + "\"pageNo\":{\"type\":\"integer\",\"description\":\"页码，从1开始\",\"default\":1},"
                        + "\"pageSize\":{\"type\":\"integer\",\"description\":\"每页条数\",\"default\":20},"
                        + "\"severity\":{\"type\":\"string\",\"description\":\"级别码：3紧急/2错误/1警告/0恢复，可空\"},"
                        + "\"status\":{\"type\":\"string\",\"description\":\"状态过滤；不传=未关闭；all=全部；支持中文/英文/数字码\"},"
                        + "\"entityName\":{\"type\":\"string\",\"description\":\"对象名过滤，如 test02，可空\"}"
                        + "}}"));

        tools.add(tool(TOOL_DETAIL,
                "查询单条告警详情。",
                "{"
                        + "\"type\":\"object\","
                        + "\"properties\":{"
                        + "\"incidentId\":{\"type\":\"string\",\"description\":\"告警ID\"}"
                        + "},"
                        + "\"required\":[\"incidentId\"]}"));

        tools.add(tool(TOOL_SIMILAR,
                "查找与指定告警相似的告警（按对象名+名称维度近似匹配，排除自身）。",
                "{"
                        + "\"type\":\"object\","
                        + "\"properties\":{"
                        + "\"incidentId\":{\"type\":\"string\",\"description\":\"基准告警ID\"},"
                        + "\"topN\":{\"type\":\"integer\",\"description\":\"返回条数\",\"default\":10}"
                        + "},"
                        + "\"required\":[\"incidentId\"]}"));

        // RAG 启用时才暴露知识检索工具（Phase 1d）
        if (ragProperties.isEnabled()) {
            tools.add(tool(TOOL_SEARCH_KNOWLEDGE,
                    "检索运维知识库（告警状态说明、级别响应、常见告警处置 SOP 等），"
                            + "返回最相关的若干片段及来源。研判告警、给处置建议或回答运维知识问题时优先调用。",
                    "{"
                            + "\"type\":\"object\","
                            + "\"properties\":{"
                            + "\"query\":{\"type\":\"string\",\"description\":\"自然语言查询，如『CPU使用率高怎么处理』\"},"
                            + "\"topK\":{\"type\":\"integer\",\"description\":\"返回片段条数\",\"default\":5}"
                            + "},"
                            + "\"required\":[\"query\"]}"));
        }

        return tools;
    }

    /**
     * 执行 {@code tools/call}。
     *
     * @param name      工具名
     * @param arguments 参数对象（可能为 null）
     * @return 工具结果对象（AlertCount / List&lt;AlertBrief&gt; / AlertBrief），由调用方序列化
     * @throws IllegalArgumentException 工具名未知或必填参数缺失
     */
    public Object callTool(String name, JsonObject arguments) {
        JsonObject args = arguments == null ? new JsonObject() : arguments;
        if (name == null) {
            throw new IllegalArgumentException("缺少工具名 name");
        }
        switch (name) {
            case TOOL_COUNT_TODAY:
                return alertQueryTools.countTodayAlerts(
                        getString(args, "status"),
                        getString(args, "severity"));
            case TOOL_QUERY:
                return alertQueryTools.queryAlerts(
                        getInt(args, "pageNo", 1),
                        getInt(args, "pageSize", 20),
                        getString(args, "severity"),
                        getString(args, "status"),
                        getString(args, "entityName"));
            case TOOL_DETAIL:
                return alertQueryTools.getAlertDetail(requireString(args, "incidentId"));
            case TOOL_SIMILAR:
                return alertQueryTools.findSimilarAlerts(
                        requireString(args, "incidentId"),
                        getInt(args, "topN", 10));
            case TOOL_SEARCH_KNOWLEDGE:
                return knowledgeSearchTools.searchKnowledge(
                        requireString(args, "query"),
                        getInt(args, "topK", 5));
            default:
                throw new IllegalArgumentException("未知工具：" + name);
        }
    }

    private static JsonObject tool(String name, String description, String inputSchemaJson) {
        JsonObject t = new JsonObject();
        t.addProperty("name", name);
        t.addProperty("description", description);
        t.add("inputSchema", GSON.fromJson(inputSchemaJson, JsonObject.class));
        return t;
    }

    private static String getString(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return o.get(key).toString();
        }
    }

    private static String requireString(JsonObject o, String key) {
        String v = getString(o, key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少必填参数：" + key);
        }
        return v.trim();
    }

    private static int getInt(JsonObject o, String key, int defaultValue) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Exception e) {
            try {
                return Integer.parseInt(o.get(key).getAsString().trim());
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
    }
}
