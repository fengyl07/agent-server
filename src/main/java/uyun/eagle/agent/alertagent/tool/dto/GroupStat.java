package uyun.eagle.agent.alertagent.tool.dto;

import lombok.Data;

/**
 * 告警分组统计的单组结果。
 *
 * <p>由 {@code /v2/incident/statistics} 返回的 {@code {value, count}} 转换而来：
 * {@link #rawValue} 保留后端原始分组值（severity/status 维度为数字码），
 * {@link #label} 为便于展示的可读值（级别/状态已翻译成中文，其余原样）。
 */
@Data
public class GroupStat {

    /** 可读分组名（severity/status 已翻译为中文，其余为原始值） */
    private String label;

    /** 后端原始分组值（severity/status 维度为数字码，其余为原始字符串） */
    private String rawValue;

    /** 该组告警数量 */
    private long count;
}
