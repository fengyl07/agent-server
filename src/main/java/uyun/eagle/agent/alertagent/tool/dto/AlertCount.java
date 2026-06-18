package uyun.eagle.agent.alertagent.tool.dto;

import lombok.Data;

/**
 * 告警计数结果。
 */
@Data
public class AlertCount {

    /** 统计日期或区间描述，如 2026-06-18 */
    private String date;

    /** 命中的告警总数 */
    private long total;
}
