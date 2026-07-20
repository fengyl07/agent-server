package uyun.eagle.agent.alertagent.tool.dto;

import lombok.Data;

/**
 * 用户摘要（字段裁剪，用于 find_user 工具输出）。
 *
 * <p>仅保留转派场景所需的最小字段：userId 用于调用转派接口，realName/account 供 LLM 向用户回显确认。
 * 不含 apiKey、权限等敏感信息。
 */
@Data
public class UserBrief {

    /** 用户 ID（转派接口 toUserId 用） */
    private String userId;

    /** 真实姓名（回显） */
    private String realName;

    /** 登录账号（回显，辅助区分同名用户） */
    private String account;
}
