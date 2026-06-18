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
    private String entityName;
    private String entityAddr;
    private String source;
    private Long count;
    private String lastOccurTime;
    private String description;
}
