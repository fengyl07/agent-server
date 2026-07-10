package uyun.eagle.agent.alertagent.agent.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.config.MemoryProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短期会话记忆存储（Phase 1e）。
 *
 * <p>按 {@code sessionId} 在进程内存（{@link ConcurrentHashMap}）中分桶保存各会话的最近对话。
 * 纯 JVM 堆内，不写 Redis/DB，不影响既有数据；应用重启即清空。
 * 读写时惰性清理：淘汰空闲超过 TTL 的会话，并在总量超限时按最久未访问（LRU）淘汰，避免内存无限增长。
 */
@Slf4j
@Component
public class ChatMemoryStore {

    @Autowired
    private MemoryProperties memoryProperties;

    private final Map<String, ConversationMemory> sessions = new ConcurrentHashMap<>();

    /**
     * 取某会话的历史消息（正序：老→新）。未启用、sessionId 为空或无历史时返回空列表。
     */
    public List<ConversationMemory.Turn> loadHistory(String sessionId) {
        if (!memoryProperties.isEnabled() || isBlank(sessionId)) {
            return Collections.emptyList();
        }
        evict();
        ConversationMemory mem = sessions.get(sessionId);
        if (mem == null) {
            return Collections.emptyList();
        }
        return mem.recent(memoryProperties.getMaxContextChars(), memoryProperties.getMaxTurns());
    }

    /**
     * 记录一轮对话（用户问 + 助手最终回复）。未启用或 sessionId 为空时不记录。
     */
    public void record(String sessionId, String userMessage, String reply) {
        if (!memoryProperties.isEnabled() || isBlank(sessionId)) {
            return;
        }
        ConversationMemory mem = sessions.computeIfAbsent(sessionId, k -> new ConversationMemory());
        int maxTurns = memoryProperties.getMaxTurns();
        mem.append("user", userMessage, maxTurns);
        mem.append("assistant", reply, maxTurns);
        evict();
    }

    /** 清空指定会话记忆（供「清空对话」等场景调用）。 */
    public void clear(String sessionId) {
        if (!isBlank(sessionId)) {
            sessions.remove(sessionId);
        }
    }

    /** 惰性清理：先按 TTL 过期，再按 LRU 控制总量。 */
    private void evict() {
        long ttlMs = (long) Math.max(1, memoryProperties.getTtlMinutes()) * 60_000L;
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> now - e.getValue().getLastAccess() > ttlMs);

        int max = Math.max(1, memoryProperties.getMaxSessions());
        while (sessions.size() > max) {
            String oldest = null;
            long oldestAccess = Long.MAX_VALUE;
            for (Map.Entry<String, ConversationMemory> e : sessions.entrySet()) {
                long access = e.getValue().getLastAccess();
                if (access < oldestAccess) {
                    oldestAccess = access;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            sessions.remove(oldest);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
