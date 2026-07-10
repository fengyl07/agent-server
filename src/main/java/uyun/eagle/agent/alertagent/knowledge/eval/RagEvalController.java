package uyun.eagle.agent.alertagent.knowledge.eval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 召回评估入口（Phase 1d）。
 *
 * <p>实际地址 {@code /alertagent/frontapi/v1/agent/rag-eval}，与其它 agent 接口同前缀，
 * 已在 ineffectiveness-path 放行。用浏览器或 curl 触发即可跑一遍评估集，返回 Recall@1/@3/@5 报告。
 * 仅用于演示与效果跟踪，不改动任何数据。
 */
@Slf4j
@RestController
@RequestMapping("/{appcode}/frontapi/v1/agent")
public class RagEvalController {

    @Autowired
    private RagEvaluationService ragEvaluationService;

    @GetMapping("/rag-eval")
    public RagEvalResult ragEval(@RequestParam(value = "topK", required = false, defaultValue = "5") int topK) {
        log.info("[RAG-EVAL] 触发评估，topK={}", topK);
        return ragEvaluationService.evaluate(topK);
    }
}
