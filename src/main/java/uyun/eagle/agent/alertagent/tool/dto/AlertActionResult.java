package uyun.eagle.agent.alertagent.tool.dto;

import lombok.Data;

/**
 * 告警处置动作结果（接手/转派/备注/关闭/解决等写操作共用）。
 *
 * <p>作为 alert-action 系列工具（首个为 {@code accept_alert}）的返回，
 * 回传给 LLM 用于组织给用户的最终回复。只包含用于确认与展示的关键信息。
 */
@Data
public class AlertActionResult {

    /** 是否成功（依据 Alert 返回的 statusCode==200 判定） */
    private boolean success;

    /** 处置动作名称（如「接手」），用于回显 */
    private String action;

    /** 目标告警 ID（回显） */
    private String incidentId;

    /** Alert 返回的原始文本消息（成功为“请求成功”，失败含具体原因，便于如实转告） */
    private String message;
}
