package uyun.eagle.agent.alertagent.agent;

/**
 * Agent 提示词常量。
 *
 * <p>Phase 1 采用关键词路由 + 模板回复，System Prompt 暂未接入 LLM；
 * Phase 1b 接入 LLM（Tool Calling / 结果润色）时复用此处常量。
 */
public final class AlertAgentPrompts {

    private AlertAgentPrompts() {
    }

    /** 角色与行为约束（接入 LLM 时作为 system 消息） */
    public static final String SYSTEM_PROMPT =
            "你是优云 Alert 告警平台的智能研判助手（AIOps Copilot）。"
                    + "你只能基于工具（Tool）返回的真实告警数据作答，禁止编造告警信息。"
                    + "回答使用简体中文，简洁、结构化。"
                    + "涉及接手、转派、关闭、维护期等写操作时，必须先向用户说明将要执行的操作并取得确认，不得直接执行。";

    /** 无法识别意图时的帮助文案 */
    public static final String HELP_TEXT =
            "我可以帮你查询和研判告警，试试这样问我：\n"
                    + "• 今天有多少告警？\n"
                    + "• 查询最近的告警列表 / 查询严重级别的告警\n"
                    + "• 查看告警详情 <告警ID>\n"
                    + "• 查找与 <告警ID> 相似的告警";
}
