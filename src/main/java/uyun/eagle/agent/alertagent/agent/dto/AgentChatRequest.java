package uyun.eagle.agent.alertagent.agent.dto;

import lombok.Data;

/**
 * Agent 对话请求。
 */
@Data
public class AgentChatRequest {

    /** 用户的自然语言消息 */
    private String message;

    /** 会话 ID（可选，Phase 1 未做多轮记忆，仅透传回显） */
    private String sessionId;
}
