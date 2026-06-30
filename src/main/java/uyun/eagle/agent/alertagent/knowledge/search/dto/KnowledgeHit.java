package uyun.eagle.agent.alertagent.knowledge.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库检索命中片段（Phase 1d）。
 *
 * <p>由 {@code search_knowledge} 返回给 LLM/MCP：包含文档片段正文、来源文件、标题与相似度分数，
 * 便于模型据此作答并标注出处，降低幻觉。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeHit {

    /** 命中的文档片段正文 */
    private String content;

    /** 来源文件名，如 03-常见告警处理SOP.md */
    private String source;

    /** 片段所属标题（取自 Markdown 最近的标题行，可能为空） */
    private String title;

    /** 相似度分数（越大越相关） */
    private double score;
}
