package uyun.eagle.agent.alertagent.knowledge.eval;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 召回评估结果（Phase 1d）。
 *
 * <p>用固定的问答评估集（{@code rag-eval/questions.json}，每条问题标注期望命中的知识文档）跑一遍
 * {@code search_knowledge}，统计 Recall@1/@3/@5 与命中相似度，量化检索效果。
 * Recall@K = 期望来源出现在 Top-K 结果中的问题数 / 总问题数。
 */
@Data
public class RagEvalResult {

    /** 评估集问题总数 */
    private int total;

    /** 检索时取的 Top-K（评估用，通常取 5） */
    private int topK;

    /** 期望来源命中在第 1 名的问题数 */
    private int hitAt1;
    /** 期望来源命中在前 3 名的问题数 */
    private int hitAt3;
    /** 期望来源命中在前 K 名的问题数 */
    private int hitAt5;

    private double recallAt1;
    private double recallAt3;
    private double recallAt5;

    /** 所有问题 Top1 相似度的平均值（反映整体检索置信度） */
    private double avgTopScore;

    /** 命中期望来源时，命中那条的平均相似度 */
    private double avgHitScore;

    /** 评估耗时（毫秒） */
    private long costMs;

    /** 逐题明细，便于定位是哪些问题没召回对 */
    private List<Item> details = new ArrayList<>();

    @Data
    public static class Item {
        private String query;
        private String expectSource;
        /** 期望来源在结果中的名次（1-based）；未在 Top-K 命中为 -1 */
        private int hitRank;
        private boolean hitAt1;
        private boolean hitAt3;
        private boolean hitAt5;
        /** Top1 相似度 */
        private double topScore;
        /** 命中期望来源那条的相似度；未命中为 0 */
        private double expectScore;
        /** 实际返回的 Top-K 来源与相似度（便于人工核对） */
        private List<String> returned = new ArrayList<>();
    }
}
