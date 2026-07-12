package uyun.eagle.agent.alertagent.tool.dto;

import lombok.Data;

/**
 * 维护期创建结果。
 *
 * <p>作为 {@code create_maintenance} 工具的返回，回传给 LLM 用于组织给用户的最终回复。
 * 只包含用于确认与展示的关键信息，不回传敏感的完整请求体。
 */
@Data
public class MaintenanceCreateResult {

    /** 是否创建成功（依据 Alert 返回的 result 标志） */
    private boolean success;

    /** 新建维护期 ID（成功时由 Alert 返回，取自 data.id） */
    private String id;

    /** 维护期名称（回显） */
    private String name;

    /** 开始时间（毫秒时间戳，固定时段时有值） */
    private Long startTime;

    /** 结束时间（毫秒时间戳，固定时段时有值） */
    private Long endTime;

    /** Alert 返回的原始文本消息（便于失败时定位） */
    private String rawMessage;
}
