package uyun.eagle.agent.alertagent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.knowledge.search.KnowledgeSearchService;
import uyun.eagle.agent.alertagent.knowledge.search.dto.KnowledgeHit;

import java.util.List;

/**
 * 知识检索类 Tool（Phase 1d，只读）。
 *
 * <p>对 {@link KnowledgeSearchService} 的薄封装，作为 Agent 与 MCP 共用的业务能力：
 * 输入自然语言查询，返回运维知识库中最相关的若干片段（含来源与相似度），供 LLM 据此研判、给出处置建议并标注出处。
 */
@Slf4j
@Component
public class KnowledgeSearchTools {

    @Autowired
    private KnowledgeSearchService knowledgeSearchService;

    /**
     * 检索运维知识库。
     *
     * @param query 自然语言查询，如「CPU 使用率高怎么处理」
     * @param topK  返回条数；&lt;=0 时取配置默认值
     * @return 命中片段（按相似度降序），无命中或未启用时返回空列表
     */
    public List<KnowledgeHit> searchKnowledge(String query, int topK) {
        List<KnowledgeHit> hits = knowledgeSearchService.search(query, topK);
        log.info("[Tool] search_knowledge query=\"{}\" topK={} -> {} 条", query, topK, hits.size());
        return hits;
    }
}
