package uyun.eagle.agent.alertagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG（向量检索）配置（Phase 1d）。
 *
 * <p>对应配置前缀 {@code rag.*}。把运维知识 Markdown 切块、向量化后写入 Qdrant，
 * 对外暴露 {@code search_knowledge} 工具，由 LLM/MCP 按需检索相关片段（区别于 Phase 1c 的整份注入）。
 *
 * <ul>
 *     <li>Embedding：OpenAI 兼容 {@code {base-url}/embeddings}（阿里百炼 text-embedding-v4 等），api-key 走 Apollo；</li>
 *     <li>Qdrant：HTTP REST（默认 127.0.0.1:6333），同机部署，无需鉴权；</li>
 *     <li>{@code enabled=false} 时不索引、不检索，search_knowledge 不注册，行为回退到 Phase 1c。</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** 是否启用 RAG；false 时不建索引、不注册 search_knowledge 工具 */
    private boolean enabled = false;

    /** 向量检索配置 */
    private Embedding embedding = new Embedding();

    /** Qdrant 向量库配置 */
    private Qdrant qdrant = new Qdrant();

    /** 检索参数 */
    private Search search = new Search();

    /** 索引（入库）参数 */
    private Index index = new Index();

    @Data
    public static class Embedding {
        /** OpenAI 兼容基路径，结尾不带斜杠，如 https://xxx.maas.aliyuncs.com/compatible-mode/v1 */
        private String baseUrl;
        /** Embedding 服务 api-key（敏感，Apollo 注入；公司内网代理可为空） */
        private String apiKey;
        /** 模型名，如 text-embedding-v4 */
        private String model = "text-embedding-v4";
        /** 向量维度，必须与 Qdrant collection 一致 */
        private int dimension = 1024;
        /** 单批最大文本条数（阿里 text-embedding-v4 上限 10） */
        private int batchSize = 10;
        /** 连接超时（毫秒） */
        private int connectTimeout = 10000;
        /** 读取超时（毫秒） */
        private int readTimeout = 30000;
    }

    @Data
    public static class Qdrant {
        /** Qdrant 主机；同机部署用 127.0.0.1 */
        private String host = "127.0.0.1";
        /** Qdrant HTTP 端口 */
        private int port = 6333;
        /** collection 名称 */
        private String collection = "alert_agent_kb";
        /** 距离度量：Cosine / Dot / Euclid */
        private String distance = "Cosine";
        /** 连接超时（毫秒） */
        private int connectTimeout = 3000;
        /** 读取超时（毫秒） */
        private int readTimeout = 10000;
    }

    @Data
    public static class Search {
        /** 默认返回条数 Top-K */
        private int topK = 5;
        /** 最低相似度分数；低于该分数的命中将被过滤；<=0 表示不过滤 */
        private double minScore = 0.0;
    }

    @Data
    public static class Index {
        /** 是否在启动时自动重建索引 */
        private boolean autoOnStartup = true;
        /** 知识文档位置（Spring 资源通配），与 Phase 1c 共用 knowledge 目录 */
        private String location = "classpath*:knowledge/*.md";
        /** 切块目标长度（字符） */
        private int chunkSize = 600;
        /** 相邻切块重叠长度（字符） */
        private int chunkOverlap = 80;
    }
}
