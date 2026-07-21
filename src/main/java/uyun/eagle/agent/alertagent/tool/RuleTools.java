package uyun.eagle.agent.alertagent.tool;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.client.AlertOpenApiClient;
import uyun.eagle.agent.alertagent.tool.dto.RuleCreateResult;

/**
 * 告警关联规则写操作 Tool（Phase 3，写操作）。
 *
 * <p>目前只提供「创建告警关闭规则」：命中条件的告警自动置为已关闭。走 Alert 端
 * {@code POST /v2/rule/createRule}，请求体为 RuleVO（rule + action）。
 * 匹配条件采用「条件定义」ruleData（key/opt/val），不做资源 ID 解析。
 *
 * <p>安全约束：该能力仅在 Chat（LLM 编排）路径暴露，不通过 MCP 暴露给外部客户端；
 * 且约定由 LLM 在取得用户明确确认后才调用（见 System Prompt）。本类只做参数拼装、
 * 默认值兜底与必填校验。默认创建后立即启用（enableIs=true，实时推送 CEP 生效）。
 */
@Slf4j
@Component
public class RuleTools {

    private static final Gson GSON = new Gson();

    /** 规则名称最大长度（与页面一致，最多 50） */
    private static final int NAME_MAX = 50;
    /** 动作类型：关闭/删除告警 */
    private static final int ACTION_TYPE_DELETE_OR_CLOSE = 1;
    /** DelOrClose 操作：关闭（1=删除，2=关闭） */
    private static final int OPERATION_CLOSE = 2;
    /** 适用范围：实时告警 */
    private static final int TARGET_REALTIME = 0;
    /** 执行安排：任意时间均执行 */
    private static final int TIME_CONDITION_ANY = 0;
    /** 关联标识：不关联 */
    private static final int ASSOCIATED_FLAG_NONE = 0;

    @Autowired
    private AlertOpenApiClient alertOpenApiClient;

    /**
     * 创建「告警关闭」规则。
     *
     * <p>必填：{@code name}（规则名称）、{@code ruleData}（至少一条完整条件 key/opt/val）。
     * 默认：{@code enabled=true}（创建即启用、推 CEP 实时生效）、适用实时告警、任意时间执行。
     *
     * @param args 工具参数（LLM 填充），字段见 McpToolRegistry 中的 inputSchema
     * @return 创建结果，供上层组织回复
     * @throws IllegalArgumentException 必填缺失或不合法时抛出，交由 LLM 追问/纠正
     */
    public RuleCreateResult createCloseRule(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;

        String name = getString(in, "name");
        if (isBlank(name)) {
            throw new IllegalArgumentException("缺少必填参数：name（规则名称）");
        }
        name = name.trim();
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("规则名称过长（最多 " + NAME_MAX + " 个字），请缩短后重试：" + name);
        }

        String description = getString(in, "description");
        // description 后端为 @NotNull，缺省时用规则名兜底
        String desc = isBlank(description) ? name : description.trim();

        // 是否立即启用：默认 true（创建即推 CEP 生效）
        Boolean enabled = getBoolean(in, "enabled");
        boolean enableIs = enabled == null || enabled;

        JsonObject ruleData = normalizeAndValidateRuleData(getObject(in, "ruleData"));

        JsonObject rule = new JsonObject();
        rule.addProperty("name", name);
        rule.addProperty("description", desc);
        rule.addProperty("target", TARGET_REALTIME);
        rule.addProperty("enableIs", enableIs);
        rule.addProperty("timeCondition", TIME_CONDITION_ANY);
        rule.addProperty("associatedFlag", ASSOCIATED_FLAG_NONE);
        rule.addProperty("holdupIs", false);
        rule.addProperty("mergeIs", false);
        rule.addProperty("richIs", false);
        rule.add("executeTime", new JsonObject());
        JsonObject basedOnNone = new JsonObject();
        basedOnNone.add("ruleData", ruleData);
        rule.add("basedOnNone", basedOnNone);

        JsonObject action = new JsonObject();
        JsonArray type = new JsonArray();
        type.add(ACTION_TYPE_DELETE_OR_CLOSE);
        action.add("type", type);
        JsonObject delOrClose = new JsonObject();
        delOrClose.addProperty("operation", OPERATION_CLOSE);
        action.add("actionDelOrClose", delOrClose);

        JsonObject body = new JsonObject();
        body.add("rule", rule);
        body.add("action", action);

        String ruleId = alertOpenApiClient.createRule(GSON.toJson(body));
        ruleId = ruleId == null ? null : ruleId.trim().replaceAll("^\"|\"$", "");

        RuleCreateResult result = new RuleCreateResult();
        result.setRuleType("告警关闭");
        result.setName(name);
        result.setEnabled(enableIs);
        result.setRuleId(ruleId);
        boolean success = !isBlank(ruleId);
        result.setSuccess(success);
        result.setMessage(success
                ? (enableIs ? "规则已创建并启用，实时生效" : "规则已创建（未启用）")
                : "Alert 未返回规则 ID");
        if (!success) {
            throw new IllegalArgumentException("关闭规则创建失败：Alert 未返回规则 ID");
        }
        return result;
    }

    /**
     * 校验并补全 ruleData。
     *
     * <p>logic 缺省为 and；要求至少一条 key/opt/val 完整的条件（对齐 Alert 端 checkRegular）。
     * 与维护期不同，规则的 ruleData 不做 datas[0] 双写，datas 置为空数组（与页面一致）。
     */
    private JsonObject normalizeAndValidateRuleData(JsonObject ruleData) {
        if (ruleData == null) {
            throw new IllegalArgumentException("缺少必填参数：ruleData（规则匹配条件）");
        }
        String logic = getString(ruleData, "logic");
        if (isBlank(logic)) {
            logic = "and";
            ruleData.addProperty("logic", logic);
        }
        JsonArray exprs = ruleData.has("exprs") && ruleData.get("exprs").isJsonArray()
                ? ruleData.getAsJsonArray("exprs") : null;
        if (exprs == null || exprs.size() == 0) {
            throw new IllegalArgumentException("ruleData 至少需要一条匹配条件（exprs）");
        }
        for (JsonElement el : exprs) {
            if (!el.isJsonObject()) {
                throw new IllegalArgumentException("ruleData.exprs 中存在非法条件项");
            }
            JsonObject expr = el.getAsJsonObject();
            if (isBlank(getString(expr, "key"))
                    || isBlank(getString(expr, "opt"))
                    || isBlank(getString(expr, "val"))) {
                throw new IllegalArgumentException("匹配条件需完整填写 key/opt/val");
            }
        }
        if (!ruleData.has("datas") || !ruleData.get("datas").isJsonArray()) {
            ruleData.add("datas", new JsonArray());
        }
        return ruleData;
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

    private static Boolean getBoolean(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsBoolean();
        } catch (Exception e) {
            String v = getString(o, key);
            if (v == null) {
                return null;
            }
            return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
