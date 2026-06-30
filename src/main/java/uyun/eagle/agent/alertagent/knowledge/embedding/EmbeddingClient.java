package uyun.eagle.agent.alertagent.knowledge.embedding;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uyun.eagle.agent.alertagent.config.RagProperties;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文本向量化客户端（Phase 1d，OpenAI 兼容 Embeddings）。
 *
 * <p>对接阿里百炼 / 公司模型网关的 {@code {base-url}/embeddings}，把文本转成向量。
 * 自建带超时的 {@link RestTemplate}，不复用平台共享 bean；api-key 仅放 Authorization 头，日志不输出。
 *
 * <p>批量调用按 {@link RagProperties.Embedding#getBatchSize()} 分批（阿里上限 10 条/次）。
 */
@Slf4j
@Component
public class EmbeddingClient {

    private static final Gson GSON = new Gson();

    @Autowired
    private RagProperties ragProperties;

    private volatile RestTemplate restTemplate;

    /**
     * 单条文本向量化。
     *
     * @param text 文本（非空）
     * @return 向量
     * @throws EmbeddingException 调用失败或维度异常
     */
    public float[] embed(String text) {
        List<float[]> list = embedBatch(Collections.singletonList(text));
        if (list.isEmpty()) {
            throw new EmbeddingException("Embedding 返回为空");
        }
        return list.get(0);
    }

    /**
     * 批量文本向量化，自动按 batchSize 分批，保持与入参一致的顺序。
     *
     * @param texts 文本列表（非空、不含 null）
     * @return 与入参一一对应的向量列表
     * @throws EmbeddingException 调用失败
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        if (texts == null || texts.isEmpty()) {
            return result;
        }
        int batchSize = Math.max(1, ragProperties.getEmbedding().getBatchSize());
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(texts.size(), i + batchSize));
            result.addAll(callEmbeddings(batch));
        }
        return result;
    }

    private List<float[]> callEmbeddings(List<String> batch) {
        RagProperties.Embedding cfg = ragProperties.getEmbedding();
        String url = endpoint();

        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.getModel());
        if (cfg.getDimension() > 0) {
            body.addProperty("dimensions", cfg.getDimension());
        }
        JsonArray inputArr = new JsonArray();
        for (String t : batch) {
            inputArr.add(t == null ? "" : t);
        }
        body.add("input", inputArr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (cfg.getApiKey() != null && !cfg.getApiKey().trim().isEmpty()) {
            headers.setBearerAuth(cfg.getApiKey().trim());
        }

        HttpEntity<String> entity = new HttpEntity<>(GSON.toJson(body), headers);
        try {
            ResponseEntity<String> rsp = restTemplate().postForEntity(url, entity, String.class);
            String resp = rsp.getBody();
            if (resp == null || resp.isEmpty()) {
                throw new EmbeddingException("Embedding 返回为空");
            }
            return parseEmbeddings(resp, batch.size());
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Embedding] 调用失败: {}", e.getMessage());
            throw new EmbeddingException("Embedding 调用失败：" + e.getMessage(), e);
        }
    }

    /** 解析 OpenAI 兼容响应 {@code {"data":[{"index":0,"embedding":[...]}]}}，按 index 还原顺序。 */
    private List<float[]> parseEmbeddings(String resp, int expected) {
        JsonObject json = GSON.fromJson(resp, JsonObject.class);
        if (json == null || !json.has("data") || !json.get("data").isJsonArray()) {
            throw new EmbeddingException("Embedding 响应缺少 data 字段");
        }
        JsonArray data = json.getAsJsonArray("data");
        float[][] ordered = new float[data.size()][];
        for (int i = 0; i < data.size(); i++) {
            JsonObject item = data.get(i).getAsJsonObject();
            int idx = item.has("index") && !item.get("index").isJsonNull() ? item.get("index").getAsInt() : i;
            JsonArray vec = item.getAsJsonArray("embedding");
            float[] arr = new float[vec.size()];
            for (int j = 0; j < vec.size(); j++) {
                arr[j] = vec.get(j).getAsFloat();
            }
            if (idx >= 0 && idx < ordered.length) {
                ordered[idx] = arr;
            } else {
                ordered[i] = arr;
            }
        }
        List<float[]> list = new ArrayList<>(ordered.length);
        for (float[] v : ordered) {
            if (v != null) {
                list.add(v);
            }
        }
        if (list.size() != expected) {
            log.warn("[Embedding] 期望 {} 条向量，实际 {} 条", expected, list.size());
        }
        return list;
    }

    private String endpoint() {
        String base = ragProperties.getEmbedding().getBaseUrl();
        if (base == null || base.trim().isEmpty()) {
            throw new EmbeddingException("未配置 rag.embedding.base-url");
        }
        base = base.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/embeddings";
    }

    private RestTemplate restTemplate() {
        RestTemplate rt = this.restTemplate;
        if (rt == null) {
            synchronized (this) {
                rt = this.restTemplate;
                if (rt == null) {
                    RagProperties.Embedding cfg = ragProperties.getEmbedding();
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(cfg.getConnectTimeout());
                    factory.setReadTimeout(cfg.getReadTimeout());
                    rt = new RestTemplate(factory);
                    rt.getMessageConverters().add(0,
                            new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8));
                    this.restTemplate = rt;
                }
            }
        }
        return rt;
    }

    /** Embedding 调用异常 */
    public static class EmbeddingException extends RuntimeException {
        public EmbeddingException(String message) {
            super(message);
        }

        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
