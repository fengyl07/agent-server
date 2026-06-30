package uyun.eagle.agent.alertagent.knowledge.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uyun.eagle.agent.alertagent.config.RagProperties;
import uyun.eagle.agent.alertagent.knowledge.embedding.EmbeddingClient;
import uyun.eagle.agent.alertagent.knowledge.search.dto.KnowledgeHit;
import uyun.eagle.agent.alertagent.knowledge.vector.QdrantVectorStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 知识库检索服务（Phase 1d）。
 *
 * <p>{@code search_knowledge} 的核心：把查询文本向量化后到 Qdrant 做相似度检索，
 * 按 {@code rag.search.min-score} 过滤低分命中，返回 Top-K 片段。RAG 未启用或检索异常时返回空列表，
 * 不影响 Agent 主流程（降级为不带知识检索）。
 */
@Slf4j
@Service
public class KnowledgeSearchService {

    @Autowired
    private RagProperties ragProperties;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private QdrantVectorStore qdrantVectorStore;

    /**
     * 检索与查询最相关的知识片段。
     *
     * @param query 查询文本
     * @param topK  返回条数；&lt;=0 时取配置默认值
     * @return 命中片段（按相似度降序，已按 min-score 过滤）；异常或未启用返回空列表
     */
    public List<KnowledgeHit> search(String query, int topK) {
        if (!ragProperties.isEnabled()) {
            log.debug("[RAG] 未启用（rag.enabled=false），search_knowledge 返回空");
            return Collections.emptyList();
        }
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        int k = topK > 0 ? topK : ragProperties.getSearch().getTopK();
        try {
            float[] vector = embeddingClient.embed(query.trim());
            List<KnowledgeHit> hits = qdrantVectorStore.search(vector, k);
            double minScore = ragProperties.getSearch().getMinScore();
            if (minScore <= 0) {
                return hits;
            }
            List<KnowledgeHit> filtered = new ArrayList<>();
            for (KnowledgeHit h : hits) {
                if (h.getScore() >= minScore) {
                    filtered.add(h);
                }
            }
            return filtered;
        } catch (Exception e) {
            log.warn("[RAG] 知识检索失败，降级返回空：{}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
