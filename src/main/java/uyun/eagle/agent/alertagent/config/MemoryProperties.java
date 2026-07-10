package uyun.eagle.agent.alertagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短期会话记忆配置（Phase 1e）。
 *
 * <p>对应前缀 {@code agent.memory.*}。按 {@code sessionId} 在进程内存里用滑动窗口保留最近对话，
 * 请求时重建多轮上下文，让 LLM 能理解「刚才那个」「第一个」等指代。
 * 不引入 Redis/DB，不影响既有数据；{@code enabled=false} 时行为回退到无记忆（Phase 1d）。
 *
 * <p>窗口用「字符预算 + 轮数兜底」双重约束：既避免上下文膨胀（成本/延迟/长上下文注意力下降），
 * 又保证短对话全量、长对话保留最近。清理用惰性方式（读写时顺带做 TTL 过期与超量 LRU 淘汰），不依赖定时任务。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.memory")
public class MemoryProperties {

    /** 是否启用短期会话记忆；false 时回退到无记忆行为 */
    private boolean enabled = false;

    /** 注入历史的总字符预算（粗略对应 token），从最新往回累加到此上限为止 */
    private int maxContextChars = 6000;

    /** 兜底：单会话保留的最大轮数（一问一答记为一轮） */
    private int maxTurns = 20;

    /** 会话空闲过期时间（分钟），超过未访问则清理 */
    private int ttlMinutes = 60;

    /** 总会话数上限，超出按最久未访问（LRU）淘汰，防止内存无限增长 */
    private int maxSessions = 500;
}
