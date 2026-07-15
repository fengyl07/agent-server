package uyun.eagle.agent.alertagent.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.config.RagProperties;
import uyun.eagle.agent.alertagent.tool.AlertActionTools;
import uyun.eagle.agent.alertagent.tool.AlertQueryTools;
import uyun.eagle.agent.alertagent.tool.KnowledgeSearchTools;
import uyun.eagle.agent.alertagent.tool.MaintenanceTools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    static final String TOOL_CREATE_MAINTENANCE = "create_maintenance";
    static final String TOOL_ACCEPT_ALERT = "accept_alert";

    /** 写操作工具名集合：仅在 Chat（LLM）路径可用，MCP 一律拒绝暴露与调用。 */
    private static final Set<String> WRITE_TOOLS =
            Collections.unmodifiableSet(new HashSet<>(
                    Arrays.asList(TOOL_CREATE_MAINTENANCE, TOOL_ACCEPT_ALERT)));

    private static final Gson GSON = new Gson();

    @Autowired
    private AlertQueryTools alertQueryTools;

    @Autowired
    private KnowledgeSearchTools knowledgeSearchTools;

    @Autowired
    private MaintenanceTools maintenanceTools;

    @Autowired
    private AlertActionTools alertActionTools;

    @Autowired
    private RagProperties ragProperties;

    /**
     * 返回 {@code tools/list} 的工具清单（MCP 用，仅只读工具，不含任何写操作）。
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
     * 返回供 LLM（Chat 路径）使用的工具清单：只读工具 + 写操作工具（create_maintenance）。
     *
     * <p>写工具只在此暴露给 LLM，不进入 {@link #listTools()}（MCP 用），从而实现「写操作仅 Chat 可用」。
     */
    public JsonArray listLlmTools() {
        JsonArray tools = listTools();
        tools.add(createMaintenanceToolDef());
        tools.add(acceptAlertToolDef());
        return tools;
    }

    /** accept_alert 的工具定义：接手指定告警，接手人为当前 API 用户。 */
    private JsonObject acceptAlertToolDef() {
        return tool(TOOL_ACCEPT_ALERT,
                "接手指定告警（接手人为当前 API 用户）。仅『未接手』状态的告警可接手；"
                        + "必须在向用户回显将接手的告警并取得明确确认后才调用。",
                "{"
                        + "\"type\":\"object\","
                        + "\"properties\":{"
                        + "\"incidentId\":{\"type\":\"string\",\"description\":\"要接手的告警ID\"}"
                        + "},"
                        + "\"required\":[\"incidentId\"]}");
    }

    /** create_maintenance 的工具定义（name/description/inputSchema），内嵌字段与取值约束供 LLM 填参。 */
    private JsonObject createMaintenanceToolDef() {
        return tool(TOOL_CREATE_MAINTENANCE,
                "创建维护期（在指定时间段内静默匹配到的告警）。维护对象用『条件定义』ruleData 表达，"
                        + "不需要资源 ID。必须在向用户回显将创建的内容并取得明确确认后才调用。",
                "{"
                        + "\"type\":\"object\","
                        + "\"properties\":{"
                        + "\"name\":{\"type\":\"string\",\"description\":\"维护期名称，必填，最多20个字\"},"
                        + "\"description\":{\"type\":\"string\",\"description\":\"描述，可选\"},"
                        + "\"timeCondition\":{\"type\":\"integer\",\"description\":\"1=固定时段(默认)。周期性暂不支持，只传1或省略\",\"default\":1},"
                        + "\"startTime\":{\"type\":\"integer\",\"description\":\"开始时间，毫秒时间戳(东八区)，固定时段必填\"},"
                        + "\"endTime\":{\"type\":\"integer\",\"description\":\"结束时间，毫秒时间戳(东八区)，必须大于 startTime\"},"
                        + "\"advanceNotify\":{\"type\":\"integer\",\"description\":\"是否提前通知：0否(默认)/1是\",\"default\":0},"
                        + "\"skipDay\":{\"type\":\"integer\",\"description\":\"是否跨天：0否/1是，可选\"},"
                        + "\"ruleData\":{\"type\":\"object\",\"description\":\"维护对象的过滤条件(条件定义)\",\"properties\":{"
                        + "\"logic\":{\"type\":\"string\",\"enum\":[\"and\",\"or\"],\"description\":\"多条件关系，默认 and\"},"
                        + "\"exprs\":{\"type\":\"array\",\"description\":\"条件列表，至少一条\",\"items\":{\"type\":\"object\",\"properties\":{"
                        + "\"key\":{\"type\":\"string\",\"description\":\"字段：alias(告警名称)/severity(等级,数值)/appKey(来源)/entityAddr(对象或IP)/description(描述)/tag(标签)/count(次数,数值)/status(状态,数值)/networkDomain(网络域)\"},"
                        + "\"opt\":{\"type\":\"string\",\"description\":\"字符串字段用 contain/notContain/equal/notEqual/startwith/endwith/matches；数值字段(severity/count/status)用 >、>=、==、<、=<、!=（注意小于等于写作 =<）\"},"
                        + "\"val\":{\"type\":\"string\",\"description\":\"值。severity: 3紧急/2错误/1警告/0恢复；status: 0未接手/40已确认/150处理中/190已解决/255已关闭\"}"
                        + "},\"required\":[\"key\",\"opt\",\"val\"]}}"
                        + "},\"required\":[\"exprs\"]}"
                        + "},"
                        + "\"required\":[\"name\",\"ruleData\"]}");
    }

    /**
     * 执行 {@code tools/call}（Chat/LLM 路径：含读与写工具）。
     *
     * @param name      工具名
     * @param arguments 参数对象（可能为 null）
     * @return 工具结果对象（AlertCount / List&lt;AlertBrief&gt; / AlertBrief / MaintenanceCreateResult / AlertActionResult），由调用方序列化
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
            case TOOL_CREATE_MAINTENANCE:
                return maintenanceTools.createMaintenance(args);
            case TOOL_ACCEPT_ALERT:
                return alertActionTools.acceptAlert(args);
            default:
                throw new IllegalArgumentException("未知工具：" + name);
        }
    }

    /**
     * 执行只读工具（MCP 路径）：拒绝一切写操作工具，其余委托给 {@link #callTool}。
     *
     * @throws IllegalArgumentException 试图通过 MCP 调用写工具，或工具名未知
     */
    public Object callReadOnlyTool(String name, JsonObject arguments) {
        if (name != null && WRITE_TOOLS.contains(name)) {
            throw new IllegalArgumentException("工具 " + name + " 不支持通过 MCP 调用");
        }
        return callTool(name, arguments);
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
