package uyun.eagle.agent.alertagent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.client.AlertOpenApiClient;
import uyun.eagle.agent.alertagent.tool.dto.GroupStat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 告警统计分析类 Tool（只读）。
 *
 * <p>基于 Alert OpenAPI 的 {@code /v2/incident/statistics}（按维度分组计数）实现「一句话统计」，
 * 如「今天各级别告警多少」「最近哪类告警最多」「今天告警按小时分布」。
 * 结果按数量降序返回 Top N，交由上层 LLM 总结成分析结论。
 *
 * <p>内部只调用 {@link AlertOpenApiClient}，不直接写 HTTP，也不依赖数据库。
 */
@Slf4j
@Component
public class StatisticsTools {

    private static final int DEFAULT_TOP_N = 10;
    /** 已关闭状态码 */
    private static final int STATUS_CLOSED = 255;

    @Autowired
    private AlertOpenApiClient alertOpenApiClient;

    /**
     * 告警分组统计。
     *
     * @param groupBy   统计维度：级别/状态/来源/类型/标签/小时/日期/星期（也接受英文字段名），必填
     * @param timeRange 时间范围：today/yesterday/last7days/last30days（也接受中文），可空表示不限时间
     * @param begin     起始毫秒时间戳（可空；与 end 一起传时优先于 timeRange）
     * @param end       结束毫秒时间戳（可空）
     * @param topN      返回前 N 组（&lt;=0 时取默认 10）
     * @return 按数量降序的分组统计结果
     */
    public List<GroupStat> statistics(String groupBy, String timeRange, Long begin, Long end, int topN) {
        String field = normalizeGroupBy(groupBy);
        if (field == null) {
            throw new IllegalArgumentException("不支持的统计维度：" + groupBy
                    + "（支持：级别severity/状态status/来源source/类型classCode/标签tags/小时hour/日期dayOfMonth/星期week）");
        }
        int limit = topN <= 0 ? DEFAULT_TOP_N : topN;

        long[] range = resolveTimeRange(timeRange, begin, end);
        Long b = range == null ? null : range[0];
        Long e = range == null ? null : range[1];

        JsonArray arr = alertOpenApiClient.statistics(field, b, e, limit, "DESC");
        List<GroupStat> list = parse(arr, field);
        list.sort(Comparator.comparingLong(GroupStat::getCount).reversed());
        if (list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    private List<GroupStat> parse(JsonArray arr, String field) {
        List<GroupStat> list = new ArrayList<>();
        if (arr == null) {
            return list;
        }
        for (JsonElement el : arr) {
            if (el == null || !el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String value = getString(o, "value");
            GroupStat gs = new GroupStat();
            gs.setRawValue(value);
            gs.setCount(getLong(o, "count"));
            gs.setLabel(translate(field, value));
            list.add(gs);
        }
        return list;
    }

    /** 将分组维度的原始值翻译成可读名（级别/状态转中文，其余原样）。 */
    private static String translate(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            return "未知";
        }
        if ("severity".equals(field)) {
            return severityCN(parseIntSafe(value), value);
        }
        if ("status".equals(field)) {
            return statusCN(parseIntSafe(value), value);
        }
        return value;
    }

    /**
     * 归一化统计维度：接受中文/英文，映射为后端字段名；不支持时返回 null。
     */
    private static String normalizeGroupBy(String groupBy) {
        if (groupBy == null) {
            return null;
        }
        String g = groupBy.trim().toLowerCase();
        switch (g) {
            case "severity":
            case "级别":
            case "等级":
                return "severity";
            case "status":
            case "状态":
                return "status";
            case "source":
            case "来源":
                return "source";
            case "classcode":
            case "类型":
            case "告警类型":
                return "classCode";
            case "tags":
            case "tag":
            case "标签":
                return "tags";
            case "hour":
            case "小时":
                return "hour";
            case "dayofmonth":
            case "日期":
            case "天":
                return "dayOfMonth";
            case "week":
            case "星期":
            case "周":
                return "week";
            default:
                return null;
        }
    }

    /**
     * 解析时间范围为 [begin, end] 毫秒时间戳。
     *
     * <p>优先级：显式 begin/end &gt; timeRange 语义词 &gt; 不限时间(返回 null)。
     */
    private static long[] resolveTimeRange(String timeRange, Long begin, Long end) {
        if (begin != null || end != null) {
            long b = begin == null ? 0L : begin;
            long e = end == null ? System.currentTimeMillis() : end;
            return new long[]{b, e};
        }
        if (timeRange == null || timeRange.trim().isEmpty()) {
            return null;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now();
        String tr = timeRange.trim().toLowerCase();
        switch (tr) {
            case "today":
            case "今天":
            case "今日":
                return dayRange(today, today, zone);
            case "yesterday":
            case "昨天":
            case "昨日": {
                LocalDate y = today.minusDays(1);
                return dayRange(y, y, zone);
            }
            case "last7days":
            case "近7天":
            case "最近7天":
            case "近一周":
                return dayRange(today.minusDays(6), today, zone);
            case "last30days":
            case "近30天":
            case "最近30天":
            case "近一月":
                return dayRange(today.minusDays(29), today, zone);
            default:
                return null;
        }
    }

    private static long[] dayRange(LocalDate from, LocalDate to, ZoneId zone) {
        long begin = from.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = to.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli();
        return new long[]{begin, end};
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

    private static String statusCN(Integer code, String fallback) {
        if (code == null) {
            return fallback;
        }
        switch (code) {
            case 0:   return "未接手";
            case 40:  return "已确认";
            case 150: return "处理中";
            case 190: return "已解决";
            case STATUS_CLOSED: return "已关闭";
            default:  return fallback;
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
            Integer v = parseIntSafe(getString(o, key));
            return v == null ? 0L : v;
        }
    }
}
