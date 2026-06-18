package uyun.eagle.agent.alertagent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.client.AlertOpenApiClient;
import uyun.eagle.agent.alertagent.tool.dto.AlertBrief;
import uyun.eagle.agent.alertagent.tool.dto.AlertCount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警查询类 Tool（Phase 1，只读）。
 *
 * <p>这些方法是 Agent 与（后续）MCP 共用的业务能力，内部只调用 {@link AlertOpenApiClient}，
 * 不直接写 HTTP，也不依赖数据库。每个方法语义单一，便于单测与被对话/工具编排复用。
 */
@Slf4j
@Component
public class AlertQueryTools {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_SIMILAR_TOP_N = 10;

    @Autowired
    private AlertOpenApiClient alertOpenApiClient;

    /**
     * 统计今天的告警数量。
     *
     * @param status   告警状态过滤（可空）
     * @param severity 告警级别过滤（可空）
     * @return 今天命中的告警总数
     */
    public AlertCount countTodayAlerts(String status, String severity) {
        LocalDate today = LocalDate.now();
        String begin = LocalDateTime.of(today, LocalTime.MIN).format(TIME_FMT);
        String end = LocalDateTime.of(today, LocalTime.MAX.withNano(0)).format(TIME_FMT);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageNo", "1");
        params.put("pageSize", "1");
        params.put("begin", begin);
        params.put("end", end);
        putIfNotBlank(params, "status", status);
        putIfNotBlank(params, "severity", severity);

        JsonObject rsp = alertOpenApiClient.queryAlerts(params);
        AlertCount count = new AlertCount();
        count.setDate(today.toString());
        count.setTotal(getLong(rsp, "total"));
        return count;
    }

    /**
     * 告警列表查询。
     *
     * @param pageNo     页码（从 1 开始，&lt;=0 时取 1）
     * @param pageSize   每页条数（&lt;=0 时取默认 20）
     * @param severity   告警级别（可空）
     * @param status     告警状态（可空）
     * @param entityName 故障源名称（可空）
     * @return 告警摘要列表
     */
    public List<AlertBrief> queryAlerts(int pageNo, int pageSize, String severity, String status, String entityName) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageNo", String.valueOf(pageNo <= 0 ? 1 : pageNo));
        params.put("pageSize", String.valueOf(pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize));
        putIfNotBlank(params, "severity", severity);
        putIfNotBlank(params, "status", status);
        putIfNotBlank(params, "entityName", entityName);

        JsonObject rsp = alertOpenApiClient.queryAlerts(params);
        return parseRecords(rsp);
    }

    /**
     * 查询单条告警详情。
     *
     * @param incidentId 告警 ID
     * @return 告警摘要；不存在时返回 null
     */
    public AlertBrief getAlertDetail(String incidentId) {
        JsonObject rsp = alertOpenApiClient.getAlertById(incidentId);
        if (rsp == null || rsp.entrySet().isEmpty()) {
            return null;
        }
        // 详情接口直接返回告警对象本身
        JsonObject alert = rsp.has("records") ? firstRecord(rsp) : rsp;
        return alert == null ? null : toBrief(alert);
    }

    /**
     * 查找与指定告警相似的告警。
     *
     * <p>策略：先取该告警详情，再以 entityName + name 维度查询，排除自身后按发生时间返回 Top N。
     * Alert 无专用“相似度”接口，这里用查询 + 过滤近似实现。
     *
     * @param incidentId 基准告警 ID
     * @param topN       返回条数（&lt;=0 时取默认 10）
     * @return 相似告警摘要列表
     */
    public List<AlertBrief> findSimilarAlerts(String incidentId, int topN) {
        AlertBrief base = getAlertDetail(incidentId);
        if (base == null) {
            return new ArrayList<>();
        }
        int limit = topN <= 0 ? DEFAULT_SIMILAR_TOP_N : topN;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageNo", "1");
        params.put("pageSize", String.valueOf(limit + 1));
        putIfNotBlank(params, "entityName", base.getEntityName());
        putIfNotBlank(params, "name", base.getName());

        JsonObject rsp = alertOpenApiClient.queryAlerts(params);
        List<AlertBrief> result = new ArrayList<>();
        for (AlertBrief brief : parseRecords(rsp)) {
            if (base.getId() != null && base.getId().equals(brief.getId())) {
                continue;
            }
            result.add(brief);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<AlertBrief> parseRecords(JsonObject rsp) {
        List<AlertBrief> list = new ArrayList<>();
        if (rsp == null) {
            return list;
        }
        JsonElement recordsEl = rsp.get("records");
        if (recordsEl == null || recordsEl.isJsonNull()) {
            return list;
        }
        if (recordsEl.isJsonArray()) {
            JsonArray arr = recordsEl.getAsJsonArray();
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    list.add(toBrief(el.getAsJsonObject()));
                }
            }
        } else if (recordsEl.isJsonObject()) {
            list.add(toBrief(recordsEl.getAsJsonObject()));
        }
        return list;
    }

    private JsonObject firstRecord(JsonObject rsp) {
        JsonElement recordsEl = rsp.get("records");
        if (recordsEl != null && recordsEl.isJsonArray() && recordsEl.getAsJsonArray().size() > 0) {
            JsonElement first = recordsEl.getAsJsonArray().get(0);
            return first.isJsonObject() ? first.getAsJsonObject() : null;
        }
        if (recordsEl != null && recordsEl.isJsonObject()) {
            return recordsEl.getAsJsonObject();
        }
        return null;
    }

    private AlertBrief toBrief(JsonObject o) {
        AlertBrief b = new AlertBrief();
        b.setId(getString(o, "id"));
        b.setName(getString(o, "name"));
        // 优先展示中文级别/状态，回退到原始值
        b.setSeverity(firstNonBlank(getString(o, "severityCN"), getString(o, "severity")));
        b.setStatus(firstNonBlank(getString(o, "statusCN"), getString(o, "status")));
        b.setEntityName(getString(o, "entityName"));
        b.setEntityAddr(getString(o, "entityAddr"));
        b.setSource(getString(o, "source"));
        b.setCount(o.has("count") && !o.get("count").isJsonNull() ? parseLongSafe(getString(o, "count")) : null);
        b.setLastOccurTime(getString(o, "lastOccurTime"));
        b.setDescription(getString(o, "description"));
        return b;
    }

    private static void putIfNotBlank(Map<String, String> params, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            params.put(key, value.trim());
        }
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

    private static long getLong(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return 0L;
        }
        try {
            return o.get(key).getAsLong();
        } catch (Exception e) {
            return parseLongSafe(getString(o, key));
        }
    }

    private static Long parseLongSafe(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.trim().isEmpty()) ? a : b;
    }
}
