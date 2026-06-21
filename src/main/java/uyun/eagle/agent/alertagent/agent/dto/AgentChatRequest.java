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

    /**
     * 列表查询的状态过滤（可选）。优先级高于从 message 中识别到的关键词。
     * 取值：数字码 / 中文（未接手、已确认、处理中、已解决、已关闭）/ 英文 / all（全部）。
     * 不传时默认只查未关闭。
     */
    private String status;

    /** 列表查询的对象名过滤（可选）。优先级高于从 message 中识别到的对象名。 */
    private String entityName;
}
