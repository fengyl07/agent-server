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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_SIMILAR_TOP_N = 10;
    /** 已关闭状态码（IncidentStatus.CLOSED） */
    private static final int STATUS_CLOSED = 255;
    /** 未关闭模式下为保证过滤后仍有足量数据，向后端多取的上限 */
    private static final int MAX_FETCH_SIZE = 200;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        ZoneId zone = ZoneId.systemDefault();
        // Alert OpenAPI 的 begin/end 要求毫秒时间戳（后端 IncidentQueryParam.begin/end 为 Long）
        long begin = today.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageNo", "1");
        params.put("pageSize", "1");
        params.put("begin", String.valueOf(begin));
        params.put("end", String.valueOf(end));
        putIfInteger(params, "status", status);
        putIfInteger(params, "severity", severity);

        JsonObject rsp = alertOpenApiClient.queryAlerts(params);
        AlertCount count = new AlertCount();
        count.setDate(today.toString());
        count.setTotal(getLong(rsp, "total"));
        return count;
    }

    /**
     * 告警列表查询。
     *
     * <p>默认行为（status 为空时）：只返回<b>未关闭</b>的告警；按<b>最后发生时间倒序</b>排列。
     * status 可传：数字状态码、中文（未接手/已确认/处理中/已解决/已关闭）、英文（new/closed...）、
     * 或 {@code all}/{@code 全部}（表示包含已关闭在内的所有状态）。
     *
     * @param pageNo     页码（从 1 开始，&lt;=0 时取 1）
     * @param pageSize   每页条数（&lt;=0 时取默认 20）
     * @param severity   告警级别（可空，仅接受数字码）
     * @param status     告警状态（可空，见上）
     * @param entityName 故障源名称（可空，按对象名过滤）
     * @return 告警摘要列表
     */
    public List<AlertBrief> queryAlerts(int pageNo, int pageSize, String severity, String status, String entityName) {
        int size = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        String s = status == null ? null : status.trim();
        boolean queryAll = s != null && (s.equalsIgnoreCase("all") || "全部".equals(s));
        Integer statusCode = queryAll ? null : parseStatusCode(s);
        // 既不是“全部”、也没有识别出明确状态码时，默认只查未关闭
        boolean defaultUnclosed = !queryAll && statusCode == null;
        // 未关闭模式需要本地再过滤掉已关闭，故向后端多取一些以保证结果数量
        int fetchSize = defaultUnclosed ? Math.min(size * 3, MAX_FETCH_SIZE) : size;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageNo", String.valueOf(pageNo <= 0 ? 1 : pageNo));
        params.put("pageSize", String.valueOf(fetchSize));
        putIfInteger(params, "severity", severity);
        if (statusCode != null) {
            params.put("status", String.valueOf(statusCode));
        }
        putIfNotBlank(params, "entityName", entityName);

        JsonObject rsp = alertOpenApiClient.queryAlerts(params);
        List<AlertBrief> list = parseRecords(rsp);

        if (defaultUnclosed) {
            list.removeIf(b -> b.getStatusCode() != null && b.getStatusCode() == STATUS_CLOSED);
        }
        // lastOccurTime 已统一格式化为 yyyy-MM-dd HH:mm:ss，字典序即时间序，可直接字符串倒序
        list.sort(Comparator.comparing(AlertBrief::getLastOccurTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (list.size() > size) {
            return new ArrayList<>(list.subList(0, size));
        }
        return list;
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
        // getIncidentById 返回 { message, statusCode, data:<Alert> }，告警对象在 data 下
        JsonObject alert = rsp;
        if (rsp.has("data") && rsp.get("data").isJsonObject()) {
            alert = rsp.get("data").getAsJsonObject();
        } else if (rsp.has("records")) {
            alert = firstRecord(rsp);
        }
        if (alert == null || alert.entrySet().isEmpty()) {
            return null;
        }
        return toBrief(alert);
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
        // 级别：优先用接口给的中文，其次按数字码映射，最后回退原始值
        Integer sevCode = getInteger(o, "severity");
        b.setSeverity(firstNonBlank(getString(o, "severityCN"), severityCN(sevCode, getString(o, "severity"))));
        // 状态：同上；并保留原始状态码用于精确过滤
        Integer stCode = getInteger(o, "status");
        b.setStatusCode(stCode);
        b.setStatus(firstNonBlank(getString(o, "statusCN"), statusCN(stCode, getString(o, "status"))));
        b.setEntityName(getString(o, "entityName"));
        b.setEntityAddr(getString(o, "entityAddr"));
        b.setSource(getString(o, "source"));
        b.setCount(o.has("count") && !o.get("count").isJsonNull() ? parseLongSafe(getString(o, "count")) : null);
        b.setLastOccurTime(formatTime(getString(o, "lastOccurTime")));
        b.setDescription(getString(o, "description"));
        return b;
    }

    /**
     * 解析状态过滤条件为后端状态码。
     *
     * <p>支持：数字码、中文（未接手/已确认/处理中/已解决/已关闭）、英文（new/acknowledged/assigned/resolved/closed）。
     * 无法识别（含 null、未关闭、open 等模糊词）时返回 {@code null}，由调用方按“默认只查未关闭”处理。
     */
    private static Integer parseStatusCode(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
            // 继续按文本匹配
        }
        switch (v.toLowerCase()) {
            case "未接手":
            case "新发生":
            case "new":
                return 0;
            case "已确认":
            case "acknowledged":
                return 40;
            case "处理中":
            case "assigned":
            case "progressing":
                return 150;
            case "已解决":
            case "resolved":
                return 190;
            case "已关闭":
            case "关闭":
            case "closed":
                return STATUS_CLOSED;
            default:
                return null;
        }
    }

    private static String statusCN(Integer code, String fallback) {
        if (code == null) {
            return fallback;
        }
        switch (code) {
            case 0:   return "未接手";
            case 40:  return "已确认";
            case 150: return "处理中";
            case 190: return "已解决";
            case 255: return "已关闭";
            default:  return fallback;
        }
    }

    private static String severityCN(Integer code, String fallback) {
        if (code == null) {
            return fallback;
        }
        switch (code) {
            case 3: return "紧急";
            case 2: return "错误";
            case 1: return "警告";
            case 0: return "恢复";
            default: return fallback;
        }
    }

    /** 将毫秒/秒时间戳格式化为 yyyy-MM-dd HH:mm:ss；已是文本时间则原样返回。 */
    private static String formatTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String v = raw.trim();
        try {
            long millis = Long.parseLong(v);
            if (v.length() <= 10) {
                millis *= 1000L;
            }
            return TIME_FMT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
        } catch (NumberFormatException e) {
            return v;
        }
    }

    private static Integer getInteger(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Exception e) {
            return parseIntSafe(getString(o, key));
        }
    }

    private static Integer parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void putIfNotBlank(Map<String, String> params, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            params.put(key, value.trim());
        }
    }

    /**
     * 仅当 value 是纯数字时才下传。
     *
     * <p>Alert 后端 {@code IncidentQueryParam} 的 severity/status 是 Integer，
     * 传中文/英文（如“严重”“已恢复”）会触发 400 类型转换异常，故非数字一律跳过。
     */
    private static void putIfInteger(Map<String, String> params, String key, String value) {
        if (value == null) {
            return;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return;
        }
        try {
            Integer.parseInt(v);
            params.put(key, v);
        } catch (NumberFormatException e) {
            log.warn("[AlertQueryTools] 参数 {} 非数字，已忽略: {}", key, v);
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
