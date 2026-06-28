package uyun.eagle.agent.alertagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 运维知识库配置（Phase 1c）。
 *
 * <p>对应配置前缀 {@code knowledge.*}。启动时把 {@link #location} 匹配到的 Markdown 文档
 * 读入并整体拼接为领域知识，注入到 LLM 的 system 消息中，提升告警研判的业务贴合度。
 *
 * <p>这是“整份文档注入”而非向量检索（RAG）；文档规模应控制在 {@link #maxChars} 以内，避免占用过多上下文。
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {

    /** 是否启用知识库注入；false 时不读取任何文档，system 仅含基础约束 */
    private boolean enabled = true;

    /** 知识文档位置（Spring 资源通配），默认加载 classpath 下 knowledge 目录的所有 md */
    private String location = "classpath*:knowledge/*.md";

    /** 注入上限（字符数）。超过则截断，防止上下文过长拖慢/超额；0 或负数表示不限制 */
    private int maxChars = 8000;
}
