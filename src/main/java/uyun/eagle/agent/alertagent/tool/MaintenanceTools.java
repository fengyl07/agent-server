package uyun.eagle.agent.alertagent.tool;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.client.AlertOpenApiClient;
import uyun.eagle.agent.alertagent.tool.dto.MaintenanceCreateResult;

/**
 * 维护期写操作 Tool（Phase 2，写操作）。
 *
 * <p>目前只提供「创建维护期」。走 Alert 端 {@code POST /v2/maintenance/create}，
 * 维护对象采用「条件定义」（ruleData 过滤条件），不做资源 ID（ciIds）解析。
 *
 * <p>安全约束：该能力仅在 Chat（LLM 编排）路径暴露，不通过 MCP 暴露给外部客户端；
 * 且约定由 LLM 在取得用户明确确认后才调用（见 System Prompt）。本类只做参数拼装、
 * 默认值兜底与必填校验，不做二次确认逻辑。
 */
@Slf4j
@Component
public class MaintenanceTools {

    private static final Gson GSON = new Gson();

    /** 名称最大长度（与 Alert 端 MaintainCreateParam 的 @Size(max=20) 对齐） */
    private static final int NAME_MAX = 20;
    /** 固定时间点执行 */
    private static final int TIME_CONDITION_FIXED = 1;
    /** 不提前通知（默认；同时规避 Alert 端 advanceNotify 拆箱空指针） */
    private static final int ADVANCE_NOTIFY_OFF = 0;
    /** 不跨天（默认） */
    private static final int SKIP_DAY_OFF = 0;
    /** 通知方式：不通知（默认；避免 Alert 端缺省导致的异常或误发通知） */
    private static final int NOTIFY_MODE_NONE = 3;
    /** 通知类型：默认 0 */
    private static final int NOTIFY_TYPE_DEFAULT = 0;
    /** 是否通知资源负责人：否（默认） */
    private static final int NOTIFY_CI_OWNER_OFF = 0;

    @Autowired
    private AlertOpenApiClient alertOpenApiClient;

    /**
     * 创建维护期（静默）。
     *
     * <p>必填：{@code name}、{@code startTime}、{@code endTime}（固定时段）、{@code ruleData}（至少一条完整条件）。
     * 默认值：{@code timeCondition=1}（固定时段单次）、{@code advanceNotify=0}（不提前通知）。
     *
     * @param args 工具参数（LLM 填充），字段见 McpToolRegistry 中的 inputSchema
     * @return 创建结果，供上层组织回复
     * @throws IllegalArgumentException 必填缺失或不合法时抛出，交由 LLM 追问/纠正
     */
    public MaintenanceCreateResult createMaintenance(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;

        String name = getString(in, "name");
        if (isBlank(name)) {
            throw new IllegalArgumentException("缺少必填参数：name（维护期名称）");
        }
        name = name.trim();
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("维护期名称过长（最多 " + NAME_MAX + " 个字），请缩短后重试：" + name);
        }

        Integer timeCondition = getInt(in, "timeCondition");
        if (timeCondition == null) {
            timeCondition = TIME_CONDITION_FIXED;
        }

        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        String description = getString(in, "description");
        if (!isBlank(description)) {
            body.addProperty("description", description.trim());
        }
        body.addProperty("timeCondition", timeCondition);

        Long startTime = getLong(in, "startTime");
        Long endTime = getLong(in, "endTime");
        if (timeCondition == TIME_CONDITION_FIXED) {
            if (startTime == null || endTime == null) {
                throw new IllegalArgumentException("固定时段维护期需要 startTime 和 endTime（毫秒时间戳）");
            }
            if (startTime >= endTime) {
                throw new IllegalArgumentException("开始时间必须早于结束时间");
            }
            body.addProperty("startTime", startTime);
            body.addProperty("endTime", endTime);
        }

        // 默认关闭提前通知（Alert 端 advanceNotify 会拆箱比较，必须非空）
        Integer advanceNotify = getInt(in, "advanceNotify");
        body.addProperty("advanceNotify", advanceNotify == null ? ADVANCE_NOTIFY_OFF : advanceNotify);

        // 是否跨天（默认不跨天）
        Integer skipDay = getInt(in, "skipDay");
        body.addProperty("skipDay", skipDay == null ? SKIP_DAY_OFF : skipDay);

        // 通知相关默认值：缺省即“不通知/无接收人”，避免 Alert 端异常或误发通知
        body.addProperty("notifyMode", NOTIFY_MODE_NONE);
        body.addProperty("notifyType", NOTIFY_TYPE_DEFAULT);
        body.addProperty("notifyUserIds", "");
        body.addProperty("notifyUserGroups", "");
        body.addProperty("notifyRoleIds", "");
        body.addProperty("notifyCiOwner", NOTIFY_CI_OWNER_OFF);

        // 维护对象：条件定义 ruleData（必填，且至少一条完整条件）
        // 固定时段创建走 OpenApi checkRegular()，要求顶层 exprs 非空；
        // 而前端详情组件读 datas[0].exprs。故双写：顶层与 datas[0] 各写一份相同条件。
        JsonObject ruleData = normalizeAndValidateRuleData(getObject(in, "ruleData"));
        body.add("ruleData", ruleData);

        JsonObject rsp = alertOpenApiClient.createMaintenance(GSON.toJson(body));

        MaintenanceCreateResult result = new MaintenanceCreateResult();
        result.setName(name);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setRawMessage(getString(rsp, "message"));
        result.setId(extractId(rsp));
        result.setSuccess(isSuccess(rsp));
        if (!result.isSuccess()) {
            String msg = result.getRawMessage();
            throw new IllegalArgumentException("维护期创建失败：" + (isBlank(msg) ? "Alert 返回未成功" : msg));
        }
        return result;
    }

    /**
     * 校验并补全 ruleData，并做「双写」以兼容 Alert 端固定时段创建与前端详情渲染。
     *
     * <p>校验：logic 缺省为 and；要求至少一条 key/opt/val 完整的条件。
     *
     * <p>双写：OpenApi 固定时段创建的 checkRegular() 要求<b>顶层 exprs</b> 非空，
     * 而前端维护期详情组件读取 <b>datas[0].exprs</b>。若只写其一，会出现
     * “创建成功但页面加载失败”或“500 请完善规则条件信息”。因此把顶层的
     * logic + exprs 深拷贝一份到 datas[0]，两处保持相同条件。
     */
    private JsonObject normalizeAndValidateRuleData(JsonObject ruleData) {
        if (ruleData == null) {
            throw new IllegalArgumentException("缺少必填参数：ruleData（维护对象的过滤条件）");
        }
        String logic = getString(ruleData, "logic");
        if (isBlank(logic)) {
            logic = "and";
            ruleData.addProperty("logic", logic);
        }
        JsonArray exprs = ruleData.has("exprs") && ruleData.get("exprs").isJsonArray()
                ? ruleData.getAsJsonArray("exprs") : null;
        if (exprs == null || exprs.size() == 0) {
            throw new IllegalArgumentException("ruleData 至少需要一条过滤条件（exprs）");
        }
        for (JsonElement el : exprs) {
            if (!el.isJsonObject()) {
                throw new IllegalArgumentException("ruleData.exprs 中存在非法条件项");
            }
            JsonObject expr = el.getAsJsonObject();
            if (isBlank(getString(expr, "key"))
                    || isBlank(getString(expr, "opt"))
                    || isBlank(getString(expr, "val"))) {
                throw new IllegalArgumentException("过滤条件需完整填写 key/opt/val");
            }
        }

        // 双写：用顶层 logic + exprs（深拷贝）构造 datas[0]，供前端详情渲染
        JsonObject nested = new JsonObject();
        nested.addProperty("logic", logic);
        nested.add("exprs", exprs.deepCopy());
        nested.add("datas", new JsonArray());

        JsonArray datas = new JsonArray();
        datas.add(nested);
        ruleData.add("datas", datas);

        return ruleData;
    }

    /** 成功判定：优先看 ResultMessage 的 result 布尔；缺省再看 errCode==200。 */
    private static boolean isSuccess(JsonObject rsp) {
        if (rsp == null) {
            return false;
        }
        if (rsp.has("result") && !rsp.get("result").isJsonNull()) {
            try {
                return rsp.get("result").getAsBoolean();
            } catch (Exception ignored) {
                // 继续按 errCode 判断
            }
        }
        if (rsp.has("errCode") && !rsp.get("errCode").isJsonNull()) {
            try {
                return rsp.get("errCode").getAsInt() == 200;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    /** 从 ResultMessage 的 data.id 取新建维护期 ID；取不到返回 null。 */
    private static String extractId(JsonObject rsp) {
        if (rsp == null || !rsp.has("data") || !rsp.get("data").isJsonObject()) {
            return null;
        }
        JsonObject data = rsp.getAsJsonObject("data");
        return getString(data, "id");
    }

    private static JsonObject getObject(JsonObject o, String key) {
        if (o == null || !o.has(key) || !o.get(key).isJsonObject()) {
            return null;
        }
        return o.getAsJsonObject(key);
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

    private static Integer getInt(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Exception e) {
            try {
                return Integer.parseInt(o.get(key).getAsString().trim());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static Long getLong(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsLong();
        } catch (Exception e) {
            try {
                return Long.parseLong(o.get(key).getAsString().trim());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
