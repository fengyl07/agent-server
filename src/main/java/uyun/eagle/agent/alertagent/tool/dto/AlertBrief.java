package uyun.eagle.agent.alertagent.tool.dto;

import lombok.Data;

/**
 * 告警摘要（字段裁剪，用于 Tool 输出与对话展示，控制 token 体积）。
 */
@Data
public class AlertBrief {

    private String id;
    private String name;
    private String severity;
    private String status;
    /** 原始状态码（0未接手/40已确认/150处理中/190已解决/255已关闭），用于精确过滤，可空 */
    private Integer statusCode;
    private String entityName;
    private String entityAddr;
    private String source;
    private Long count;
    /** 最后发生时间，已格式化为 yyyy-MM-dd HH:mm:ss（字典序即时间序，便于排序展示） */
    private String lastOccurTime;
    private String description;
}
