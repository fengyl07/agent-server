package uyun.eagle.agent.alertagent.knowledge.eval;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uyun.eagle.agent.alertagent.config.RagProperties;
import uyun.eagle.agent.alertagent.knowledge.search.KnowledgeSearchService;
import uyun.eagle.agent.alertagent.knowledge.search.dto.KnowledgeHit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG 召回评估服务（Phase 1d）。
 *
 * <p>加载评估集 {@code rag-eval/questions.json}，对每条问题调用 {@link KnowledgeSearchService} 检索 Top-K，
 * 判断期望命中的知识文档是否出现在前 1/3/K 名，聚合出 Recall@1/@3/@5 与命中相似度。
 * 这是把「RAG 效果」从口头描述变成可复现数字的最小闭环，便于演示与后续持续跟踪。
 */
@Slf4j
@Service
public class RagEvaluationService {

    private static final Gson GSON = new Gson();
    private static final String EVAL_SET = "rag-eval/questions.json";

    @Autowired
    private RagProperties ragProperties;

    @Autowired
    private KnowledgeSearchService knowledgeSearchService;

    /** 评估集单条：一个自然语言问题 + 期望命中的知识文档文件名。 */
    @Data
    public static class EvalCase {
        private String query;
        private String expectSource;
        private String note;
    }

    /**
     * 跑一遍评估集，返回召回率报告。
     *
     * @param topK 检索取的条数；&lt;=0 时取默认 5
     */
    public RagEvalResult evaluate(int topK) {
        if (!ragProperties.isEnabled()) {
            throw new IllegalStateException("RAG 未启用（rag.enabled=false），无法评估");
        }
        int k = topK > 0 ? topK : Math.max(5, ragProperties.getSearch().getTopK());
        List<EvalCase> cases = loadCases();
        if (cases.isEmpty()) {
            throw new IllegalStateException("评估集为空：" + EVAL_SET);
        }

        long t0 = System.currentTimeMillis();
        RagEvalResult result = new RagEvalResult();
        result.setTopK(k);
        result.setTotal(cases.size());

        int hit1 = 0;
        int hit3 = 0;
        int hit5 = 0;
        double sumTopScore = 0;
        double sumHitScore = 0;
        int hitScoreCount = 0;

        for (EvalCase c : cases) {
            List<KnowledgeHit> hits = knowledgeSearchService.search(c.getQuery(), k);

            RagEvalResult.Item item = new RagEvalResult.Item();
            item.setQuery(c.getQuery());
            item.setExpectSource(c.getExpectSource());

            int rank = -1;
            double expectScore = 0;
            for (int i = 0; i < hits.size(); i++) {
                KnowledgeHit h = hits.get(i);
                item.getReturned().add(describe(h));
                if (rank < 0 && c.getExpectSource() != null && c.getExpectSource().equals(h.getSource())) {
                    rank = i + 1;
                    expectScore = h.getScore();
                }
            }

            double topScore = hits.isEmpty() ? 0 : hits.get(0).getScore();
            item.setTopScore(round(topScore));
            item.setExpectScore(round(expectScore));
            item.setHitRank(rank);
            boolean at1 = rank == 1;
            boolean at3 = rank >= 1 && rank <= 3;
            boolean at5 = rank >= 1 && rank <= k;
            item.setHitAt1(at1);
            item.setHitAt3(at3);
            item.setHitAt5(at5);

            if (at1) {
                hit1++;
            }
            if (at3) {
                hit3++;
            }
            if (at5) {
                hit5++;
                sumHitScore += expectScore;
                hitScoreCount++;
            }
            sumTopScore += topScore;

            result.getDetails().add(item);
        }

        int total = cases.size();
        result.setHitAt1(hit1);
        result.setHitAt3(hit3);
        result.setHitAt5(hit5);
        result.setRecallAt1(round((double) hit1 / total));
        result.setRecallAt3(round((double) hit3 / total));
        result.setRecallAt5(round((double) hit5 / total));
        result.setAvgTopScore(round(sumTopScore / total));
        result.setAvgHitScore(round(hitScoreCount == 0 ? 0 : sumHitScore / hitScoreCount));
        result.setCostMs(System.currentTimeMillis() - t0);

        log.info("[RAG-EVAL] total={} Recall@1={} Recall@3={} Recall@5={} avgTopScore={} avgHitScore={} cost={}ms",
                total, result.getRecallAt1(), result.getRecallAt3(), result.getRecallAt5(),
                result.getAvgTopScore(), result.getAvgHitScore(), result.getCostMs());
        return result;
    }

    private List<EvalCase> loadCases() {
        try (InputStream in = new ClassPathResource(EVAL_SET).getInputStream();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<EvalCase>>() {
            }.getType();
            List<EvalCase> cases = GSON.fromJson(reader, type);
            return cases == null ? new ArrayList<>() : cases;
        } catch (Exception e) {
            throw new IllegalStateException("加载评估集失败：" + EVAL_SET + "，" + e.getMessage(), e);
        }
    }

    private static String describe(KnowledgeHit h) {
        return h.getSource() + " # " + h.getTitle() + " (" + round(h.getScore()) + ")";
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
