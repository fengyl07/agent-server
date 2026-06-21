package uyun.eagle.agent.alertagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uyun.eagle.agent.alertagent.agent.dto.AgentChatRequest;
import uyun.eagle.agent.alertagent.agent.dto.AgentChatResponse;
import uyun.eagle.agent.alertagent.tool.AlertQueryTools;
import uyun.eagle.agent.alertagent.tool.dto.AlertBrief;
import uyun.eagle.agent.alertagent.tool.dto.AlertCount;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 告警 Agent 编排服务（Phase 1：关键词路由）。
 *
 * <p>当前不依赖 LLM：根据用户消息识别意图 -> 调用 {@link AlertQueryTools} -> 模板化输出。
 * Phase 1b 接入 LLM 后，可在此处替换为 Tool Calling，并用 LLM 对工具结果做润色与研判。
 */
@Slf4j
@Service
public class AlertAgentService {

    /** 匹配告警 ID：24~32 位十六进制串 */
    private static final Pattern INCIDENT_ID = Pattern.compile("[0-9a-fA-F]{24,32}");
    /** 从消息中识别对象名：显式“对象/来源/主机 xxx”，或独立的 testNN（当前测试数据约定） */
    private static final Pattern ENTITY_EXPLICIT = Pattern.compile("(?:对象|来源对象|来源|主机|entity)\\s*[:：]?\\s*([A-Za-z0-9_.\\-]+)");
    private static final Pattern ENTITY_TESTCASE = Pattern.compile("\\btest[0-9A-Za-z_]+\\b");

    @Autowired
    private AlertQueryTools alertQueryTools;

    /**
     * 处理一次对话。结构化字段（{@link AgentChatRequest#getStatus()} / {@code entityName}）
     * 优先级高于从消息文本中识别到的关键词。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        String message = request == null ? null : request.getMessage();
        String sessionId = request == null ? null : request.getSessionId();
        if (message == null || message.trim().isEmpty()) {
            return new AgentChatResponse(AlertAgentPrompts.HELP_TEXT, sessionId, "help");
        }
        String text = message.trim();
        String incidentId = extractIncidentId(text);
        String reqStatus = request == null ? null : request.getStatus();
        String reqEntityName = request == null ? null : request.getEntityName();

        try {
            // 1) 相似告警（需要 ID）
            if (containsAny(text, "相似", "类似") && incidentId != null) {
                List<AlertBrief> similar = alertQueryTools.findSimilarAlerts(incidentId, 10);
                return new AgentChatResponse(renderSimilar(incidentId, similar), sessionId, "similar");
            }

            // 2) 告警详情（需要 ID）
            if (incidentId != null && containsAny(text, "详情", "明细", "detail", "查看", "详细")) {
                AlertBrief detail = alertQueryTools.getAlertDetail(incidentId);
                return new AgentChatResponse(renderDetail(incidentId, detail), sessionId, "detail");
            }

            // 3) 今天告警数量
            if (containsAny(text, "今天", "今日") && text.contains("告警")
                    && containsAny(text, "多少", "数量", "几条", "几个", "总数")) {
                AlertCount count = alertQueryTools.countTodayAlerts(null, null);
                return new AgentChatResponse(renderCount(count), sessionId, "count");
            }

            // 4) 告警列表（默认只查未关闭 + 按最后发生时间倒序；支持状态/对象名过滤）
            if (containsAny(text, "列表", "有哪些", "最近", "看看告警", "查询告警", "告警列表", "查告警")) {
                String status = firstNonBlank(reqStatus, statusFromText(text));
                String entityName = firstNonBlank(reqEntityName, entityFromText(text));
                List<AlertBrief> alerts = alertQueryTools.queryAlerts(1, 20, null, status, entityName);
                return new AgentChatResponse(renderList(alerts, status, entityName), sessionId, "list");
            }

            // 5) 只给了 ID，默认查详情
            if (incidentId != null) {
                AlertBrief detail = alertQueryTools.getAlertDetail(incidentId);
                return new AgentChatResponse(renderDetail(incidentId, detail), sessionId, "detail");
            }

            return new AgentChatResponse(AlertAgentPrompts.HELP_TEXT, sessionId, "help");
        } catch (Exception e) {
            log.error("[AlertAgent] 处理对话失败: {}", e.getMessage(), e);
            return new AgentChatResponse("抱歉，查询告警时出错了：" + e.getMessage(), sessionId, "error");
        }
    }

    private String renderCount(AlertCount count) {
        return String.format("%s 共有 %d 条告警。", count.getDate(), count.getTotal());
    }

    private String renderList(List<AlertBrief> alerts, String status, String entityName) {
        String scope = describeScope(status, entityName);
        if (alerts == null || alerts.isEmpty()) {
            return "没有查询到" + scope + "告警。";
        }
        StringBuilder sb = new StringBuilder("为你查询到以下" + scope + "告警（按最近发生时间倒序，最多 20 条）：\n");
        int i = 1;
        for (AlertBrief a : alerts) {
            sb.append(i++).append(". ")
                    .append(nullToDash(a.getName()))
                    .append(" ｜级别：").append(nullToDash(a.getSeverity()))
                    .append(" ｜状态：").append(nullToDash(a.getStatus()))
                    .append(" ｜来源对象：").append(nullToDash(a.getEntityName()))
                    .append(" ｜最近发生：").append(nullToDash(a.getLastOccurTime()))
                    .append(" ｜ID：").append(nullToDash(a.getId()))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    /** 生成列表查询范围描述，如“已关闭、对象 test02 的 ”，默认为“未关闭 ”。 */
    private String describeScope(String status, String entityName) {
        StringBuilder sb = new StringBuilder();
        boolean all = status != null && (status.equalsIgnoreCase("all") || "全部".equals(status.trim()));
        if (all) {
            sb.append("全部状态");
        } else if (status != null && !status.trim().isEmpty()) {
            sb.append("“").append(status.trim()).append("”");
        } else {
            sb.append("未关闭");
        }
        if (entityName != null && !entityName.trim().isEmpty()) {
            sb.append("、对象 ").append(entityName.trim());
        }
        sb.append("的");
        return sb.toString();
    }

    private String renderDetail(String incidentId, AlertBrief d) {
        if (d == null) {
            return "未找到告警：" + incidentId;
        }
        return "告警详情：\n"
                + "• 名称：" + nullToDash(d.getName()) + "\n"
                + "• ID：" + nullToDash(d.getId()) + "\n"
                + "• 级别：" + nullToDash(d.getSeverity()) + "\n"
                + "• 状态：" + nullToDash(d.getStatus()) + "\n"
                + "• 来源对象：" + nullToDash(d.getEntityName()) + "（" + nullToDash(d.getEntityAddr()) + "）\n"
                + "• 来源：" + nullToDash(d.getSource()) + "\n"
                + "• 累计次数：" + (d.getCount() == null ? "-" : d.getCount()) + "\n"
                + "• 最近发生：" + nullToDash(d.getLastOccurTime()) + "\n"
                + "• 描述：" + nullToDash(d.getDescription());
    }

    private String renderSimilar(String incidentId, List<AlertBrief> similar) {
        if (similar == null || similar.isEmpty()) {
            return "未找到与 " + incidentId + " 相似的告警。";
        }
        StringBuilder sb = new StringBuilder("与 " + incidentId + " 相似的告警（最多 10 条）：\n");
        int i = 1;
        for (AlertBrief a : similar) {
            sb.append(i++).append(". ")
                    .append(nullToDash(a.getName()))
                    .append(" ｜来源对象：").append(nullToDash(a.getEntityName()))
                    .append(" ｜最近发生：").append(nullToDash(a.getLastOccurTime()))
                    .append(" ｜ID：").append(nullToDash(a.getId()))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String extractIncidentId(String text) {
        Matcher m = INCIDENT_ID.matcher(text);
        return m.find() ? m.group() : null;
    }

    /**
     * 从消息文本识别状态过滤关键词，返回原始关键词交由工具层统一解析；识别不到返回 null（即默认未关闭）。
     */
    private static String statusFromText(String text) {
        if (text.contains("全部状态") || text.contains("所有状态") || text.contains("包含已关闭")) {
            return "all";
        }
        if (text.contains("已关闭")) {
            return "已关闭";
        }
        if (text.contains("已解决")) {
            return "已解决";
        }
        if (text.contains("处理中")) {
            return "处理中";
        }
        if (text.contains("已确认")) {
            return "已确认";
        }
        if (text.contains("未接手") || text.contains("新发生")) {
            return "未接手";
        }
        return null;
    }

    /** 从消息文本识别对象名：优先显式“对象 xxx”，其次匹配 testNN；识别不到返回 null。 */
    private static String entityFromText(String text) {
        Matcher m = ENTITY_EXPLICIT.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        Matcher t = ENTITY_TESTCASE.matcher(text);
        if (t.find()) {
            return t.group();
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.trim().isEmpty()) ? a : b;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static String nullToDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }
}
