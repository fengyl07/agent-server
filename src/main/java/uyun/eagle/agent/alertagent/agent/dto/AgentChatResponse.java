package uyun.eagle.agent.alertagent.agent.dto;

import lombok.Data;

/**
 * Agent 对话响应。
 */
@Data
public class AgentChatResponse {

    /** 自然语言回复 */
    private String reply;

    /** 会话 ID 回显 */
    private String sessionId;

    /** 命中的意图，便于调试：count / list / detail / similar / help */
    private String intent;

    public AgentChatResponse() {
    }

    public AgentChatResponse(String reply, String sessionId, String intent) {
        this.reply = reply;
        this.sessionId = sessionId;
        this.intent = intent;
    }
}
