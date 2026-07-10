package uyun.eagle.agent.alertagent.agent.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * 单个会话的短期记忆（Phase 1e）。
 *
 * <p>只保存「用户问」与「助手最终回复」的纯文本轮次，不保存中间的 tool_calls/tool 消息——
 * 以规避 OpenAI Tool Calling 的消息配对约束（带 tool_calls 的 assistant 必须紧跟配对 tool 结果），
 * 同时降低 token 占用。所有读写方法 {@code synchronized}，保证同一会话并发安全。
 */
public class ConversationMemory {

    /** 一条对话消息（role 取 user / assistant） */
    public static class Turn {
        private final String role;
        private final String content;

        public Turn(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    private final Deque<Turn> turns = new ArrayDeque<>();
    private volatile long lastAccess = System.currentTimeMillis();

    /**
     * 追加一条消息，并按最大消息数裁剪最老的（{@code maxTurns} 轮≈2*maxTurns 条）。
     *
     * @param role     user / assistant
     * @param content  文本内容；空白则忽略
     * @param maxTurns 保留的最大轮数（一问一答为一轮）
     */
    public synchronized void append(String role, String content, int maxTurns) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        turns.addLast(new Turn(role, content.trim()));
        int maxMessages = Math.max(2, maxTurns * 2);
        while (turns.size() > maxMessages) {
            turns.pollFirst();
        }
        lastAccess = System.currentTimeMillis();
    }

    /**
     * 取最近的历史消息（正序：老→新），受字符预算与轮数双重约束。
     *
     * <p>从最新往回累加 content 长度，超过 {@code maxChars} 即停；再截断到 {@code maxTurns} 轮内。
     *
     * @return 可直接顺序注入 LLM messages 的历史列表；无历史返回空列表
     */
    public synchronized List<Turn> recent(int maxChars, int maxTurns) {
        lastAccess = System.currentTimeMillis();
        List<Turn> collected = new ArrayList<>();
        int budget = maxChars > 0 ? maxChars : Integer.MAX_VALUE;
        int maxMessages = Math.max(2, maxTurns * 2);
        int used = 0;
        Iterator<Turn> it = turns.descendingIterator();
        while (it.hasNext()) {
            Turn t = it.next();
            int len = t.getContent().length();
            if (!collected.isEmpty() && used + len > budget) {
                break;
            }
            collected.add(t);
            used += len;
            if (collected.size() >= maxMessages) {
                break;
            }
        }
        // collected 目前是「新→老」，反转为「老→新」再返回
        List<Turn> ordered = new ArrayList<>(collected.size());
        for (int i = collected.size() - 1; i >= 0; i--) {
            ordered.add(collected.get(i));
        }
        return ordered;
    }

    public long getLastAccess() {
        return lastAccess;
    }
}
