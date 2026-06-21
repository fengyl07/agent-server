package uyun.eagle.agent.alertagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 接入配置（Phase 1b）。
 *
 * <p>对应配置前缀 {@code llm.*}，指向 OpenAI 兼容的模型服务（DeepSeek API / Ollama / 公司模型网关）。
 * {@code enabled=false} 时 Chat 走 Phase 1 关键词路由，不依赖任何外部模型；
 * 切换到 true 后由 LLM 做意图理解 + Tool Calling + 回复润色/研判。
 *
 * <p>api-key 为敏感信息，必须经 Apollo 注入，禁止硬编码或写入日志。
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** 是否启用 LLM 编排；false 时降级到关键词路由 */
    private boolean enabled = false;

    /** OpenAI 兼容基路径，结尾不带斜杠，如 https://api.deepseek.com/v1 */
    private String baseUrl;

    /** 模型服务 apikey（敏感，Apollo 注入） */
    private String apiKey;

    /** 模型名，如 deepseek-chat */
    private String model = "deepseek-chat";

    /** 连接超时（毫秒） */
    private int connectTimeout = 10000;

    /** 读取超时（毫秒）；LLM 生成较慢，默认放宽到 60s */
    private int readTimeout = 60000;

    /** 采样温度，0~2，越低越确定，告警研判建议偏低 */
    private double temperature = 0.2;

    /** 一次对话内允许的最大 Tool 调用轮数，防止死循环 */
    private int maxToolRounds = 5;
}
