package uyun.eagle.agent.alertagent.tool.dto;

import lombok.Data;

/**
 * 关联规则（告警关闭）创建结果。
 */
@Data
public class RuleCreateResult {

    /** 是否创建成功 */
    private boolean success;

    /** 规则类型描述，固定为「告警关闭」 */
    private String ruleType;

    /** 新建规则 ID（Alert 返回的 ruleId） */
    private String ruleId;

    /** 规则名称 */
    private String name;

    /** 是否创建后立即启用（true 时已推送 CEP 实时生效） */
    private boolean enabled;

    /** 附加信息（成功提示或失败原因） */
    private String message;
}
