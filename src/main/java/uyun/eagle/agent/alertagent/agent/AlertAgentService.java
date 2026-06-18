package uyun.eagle.agent.alertagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Autowired
    private AlertQueryTools alertQueryTools;

    /**
     * 处理一次对话。
     *
     * @param message   用户消息
     * @param sessionId 会话 ID（透传回显）
     * @return 对话响应
     */
    public AgentChatResponse chat(String message, String sessionId) {
        if (message == null || message.trim().isEmpty()) {
            return new AgentChatResponse(AlertAgentPrompts.HELP_TEXT, sessionId, "help");
        }
        String text = message.trim();
        String incidentId = extractIncidentId(text);

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

            // 4) 告警列表
            if (containsAny(text, "列表", "有哪些", "最近", "看看告警", "查询告警", "告警列表", "查告警")) {
                List<AlertBrief> alerts = alertQueryTools.queryAlerts(1, 20, null, null, null);
                return new AgentChatResponse(renderList(alerts), sessionId, "list");
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

    private String renderList(List<AlertBrief> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return "没有查询到告警。";
        }
        StringBuilder sb = new StringBuilder("为你查询到以下告警（最多 20 条）：\n");
        int i = 1;
        for (AlertBrief a : alerts) {
            sb.append(i++).append(". ")
                    .append(nullToDash(a.getName()))
                    .append(" ｜级别：").append(nullToDash(a.getSeverity()))
                    .append(" ｜状态：").append(nullToDash(a.getStatus()))
                    .append(" ｜来源对象：").append(nullToDash(a.getEntityName()))
                    .append(" ｜ID：").append(nullToDash(a.getId()))
                    .append("\n");
        }
        return sb.toString().trim();
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
