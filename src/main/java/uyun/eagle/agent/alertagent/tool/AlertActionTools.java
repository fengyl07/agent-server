package uyun.eagle.agent.alertagent.tool;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.client.AlertOpenApiClient;
import uyun.eagle.agent.alertagent.tool.dto.AlertActionResult;

/**
 * 告警处置写操作 Tool（Phase 2，alert-action）。
 *
 * <p>已提供：接手、备注、转派、关闭、解决。均走 Alert 端 {@code POST /v2/incident/*}，
 * 操作人为 apikey 对应的用户，无需调用方指定（转派需 toUserId）。关闭/解决为不可逆终态，
 * 且有状态机前置（如未接手不可解决），失败原因由 Alert 返回、如实转告用户。
 *
 * <p>安全约束：该能力仅在 Chat（LLM 编排）路径暴露，不通过 MCP 暴露给外部客户端；
 * 且约定由 LLM 在取得用户明确确认后才调用（见 System Prompt）。本类只做参数校验、
 * 调用与结果判定，不做二次确认逻辑。
 *
 * <p>注意：接手接口返回体为 {@code {"statusCode":200/500,"message":...}}，
 * 与维护期的 ResultMessage（result/errCode）不同，成功判定以 {@code statusCode==200} 为准。
 */
@Slf4j
@Component
public class AlertActionTools {

    /** 接手接口成功的 statusCode */
    private static final int STATUS_OK = 200;

    @Autowired
    private AlertOpenApiClient alertOpenApiClient;

    /**
     * 接手告警。
     *
     * <p>必填：{@code incidentId}。仅「未接手」状态的告警可接手；已接手/不存在会由 Alert 返回失败原因。
     *
     * @param args 工具参数（LLM 填充），需含 incidentId
     * @return 接手结果，供上层组织回复
     * @throws IllegalArgumentException 必填缺失或 Alert 返回失败时抛出，交由 LLM 如实转告用户
     */
    public AlertActionResult acceptAlert(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;

        String incidentId = getString(in, "incidentId");
        if (isBlank(incidentId)) {
            throw new IllegalArgumentException("缺少必填参数：incidentId（要接手的告警ID）");
        }
        incidentId = incidentId.trim();

        JsonObject rsp = alertOpenApiClient.receiveIncident(incidentId);

        boolean success = isSuccess(rsp);
        String message = getString(rsp, "message");

        AlertActionResult result = new AlertActionResult();
        result.setAction("接手");
        result.setIncidentId(incidentId);
        result.setMessage(message);
        result.setSuccess(success);
        if (!success) {
            throw new IllegalArgumentException("接手失败：" + (isBlank(message) ? "Alert 返回未成功" : message));
        }
        return result;
    }

    /**
     * 给告警添加备注。
     *
     * <p>必填：{@code incidentId}、{@code remark}。备注人为当前 API 用户。
     *
     * @param args 工具参数，需含 incidentId、remark
     * @return 备注结果
     * @throws IllegalArgumentException 必填缺失或 Alert 返回失败时抛出
     */
    public AlertActionResult addRemark(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;

        String incidentId = getString(in, "incidentId");
        if (isBlank(incidentId)) {
            throw new IllegalArgumentException("缺少必填参数：incidentId（要备注的告警ID）");
        }
        String remark = getString(in, "remark");
        if (isBlank(remark)) {
            throw new IllegalArgumentException("缺少必填参数：remark（备注内容）");
        }
        incidentId = incidentId.trim();

        JsonObject rsp = alertOpenApiClient.remarkIncident(incidentId, remark.trim());

        boolean success = isSuccess(rsp);
        String message = getString(rsp, "message");

        AlertActionResult result = new AlertActionResult();
        result.setAction("备注");
        result.setIncidentId(incidentId);
        result.setMessage(message);
        result.setSuccess(success);
        if (!success) {
            throw new IllegalArgumentException("备注失败：" + (isBlank(message) ? "Alert 返回未成功" : message));
        }
        return result;
    }

    /**
     * 转派告警给指定用户。
     *
     * <p>必填：{@code incidentId}、{@code toUserId}（由 find_user 解析得到）。
     * 当前 API 用户须为该告警负责人或管理员，否则 Alert 端会返回失败。
     *
     * @param args 工具参数，需含 incidentId、toUserId
     * @return 转派结果
     * @throws IllegalArgumentException 必填缺失或 Alert 返回失败时抛出
     */
    public AlertActionResult transferAlert(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;

        String incidentId = getString(in, "incidentId");
        if (isBlank(incidentId)) {
            throw new IllegalArgumentException("缺少必填参数：incidentId（要转派的告警ID）");
        }
        String toUserId = getString(in, "toUserId");
        if (isBlank(toUserId)) {
            throw new IllegalArgumentException("缺少必填参数：toUserId（转派目标用户ID，先用 find_user 查得）");
        }
        incidentId = incidentId.trim();

        JsonObject rsp = alertOpenApiClient.transferIncident(incidentId, toUserId.trim());

        boolean success = isSuccess(rsp);
        String message = getString(rsp, "message");

        AlertActionResult result = new AlertActionResult();
        result.setAction("转派");
        result.setIncidentId(incidentId);
        result.setMessage(message);
        result.setSuccess(success);
        if (!success) {
            throw new IllegalArgumentException("转派失败：" + (isBlank(message) ? "Alert 返回未成功" : message));
        }
        return result;
    }

    /**
     * 关闭告警（不可逆终态）。
     *
     * <p>必填：{@code incidentId}、{@code closeMessage}（关闭原因，不可省略/编造）。
     * Alert 端会校验状态：已关闭的告警会返回失败。
     *
     * @param args 工具参数，需含 incidentId、closeMessage
     * @return 关闭结果
     * @throws IllegalArgumentException 必填缺失或 Alert 返回失败时抛出
     */
    public AlertActionResult closeAlert(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;

        String incidentId = getString(in, "incidentId");
        if (isBlank(incidentId)) {
            throw new IllegalArgumentException("缺少必填参数：incidentId（要关闭的告警ID）");
        }
        String closeMessage = getString(in, "closeMessage");
        if (isBlank(closeMessage)) {
            throw new IllegalArgumentException("缺少必填参数：closeMessage（关闭原因）");
        }
        incidentId = incidentId.trim();

        JsonObject rsp = alertOpenApiClient.closeIncident(incidentId, closeMessage.trim());

        boolean success = isSuccess(rsp);
        String message = getString(rsp, "message");

        AlertActionResult result = new AlertActionResult();
        result.setAction("关闭");
        result.setIncidentId(incidentId);
        result.setMessage(message);
        result.setSuccess(success);
        if (!success) {
            throw new IllegalArgumentException("关闭失败：" + (isBlank(message) ? "Alert 返回未成功" : message));
        }
        return result;
    }

    /**
     * 解决告警（不可逆终态）。
     *
     * <p>必填：{@code incidentId}、{@code resolveMessage}（解决说明，不可省略/编造）。
     * Alert 端有状态机校验：未接手等状态不允许直接解决时会返回失败原因。
     *
     * @param args 工具参数，需含 incidentId、resolveMessage
     * @return 解决结果
     * @throws IllegalArgumentException 必填缺失或 Alert 返回失败时抛出
     */
    public AlertActionResult resolveAlert(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;

        String incidentId = getString(in, "incidentId");
        if (isBlank(incidentId)) {
            throw new IllegalArgumentException("缺少必填参数：incidentId（要解决的告警ID）");
        }
        String resolveMessage = getString(in, "resolveMessage");
        if (isBlank(resolveMessage)) {
            throw new IllegalArgumentException("缺少必填参数：resolveMessage（解决说明）");
        }
        incidentId = incidentId.trim();

        JsonObject rsp = alertOpenApiClient.resolveIncident(incidentId, resolveMessage.trim());

        boolean success = isSuccess(rsp);
        String message = getString(rsp, "message");

        AlertActionResult result = new AlertActionResult();
        result.setAction("解决");
        result.setIncidentId(incidentId);
        result.setMessage(message);
        result.setSuccess(success);
        if (!success) {
            throw new IllegalArgumentException("解决失败：" + (isBlank(message) ? "Alert 返回未成功" : message));
        }
        return result;
    }

    /** 成功判定：接手/备注/转派/关闭/解决接口均以 statusCode==200 表示成功（无 result/errCode 字段）。 */
    private static boolean isSuccess(JsonObject rsp) {
        if (rsp == null) {
            return false;
        }
        if (rsp.has("statusCode") && !rsp.get("statusCode").isJsonNull()) {
            try {
                return rsp.get("statusCode").getAsInt() == STATUS_OK;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
