package uyun.eagle.agent.alertagent.knowledge.vector;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uyun.eagle.agent.alertagent.config.RagProperties;
import uyun.eagle.agent.alertagent.knowledge.search.dto.KnowledgeHit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Qdrant 向量库访问（Phase 1d，HTTP REST）。
 *
 * <p>直接调用 Qdrant 的 HTTP API（默认 127.0.0.1:6333），不引入额外 SDK 依赖：
 * <ul>
 *     <li>{@link #recreateCollection()}：按配置维度重建 collection（用于启动全量重建索引）；</li>
 *     <li>{@link #upsert(List)}：写入向量点；</li>
 *     <li>{@link #search(float[], int)}：向量相似度检索 Top-K。</li>
 * </ul>
 */
@Slf4j
@Component
public class QdrantVectorStore {

    private static final Gson GSON = new Gson();

    @Autowired
    private RagProperties ragProperties;

    private volatile RestTemplate restTemplate;

    /** 删除并按配置维度重新创建 collection（幂等，启动重建用）。 */
    public void recreateCollection() {
        RagProperties.Qdrant cfg = ragProperties.getQdrant();
        String url = baseUrl() + "/collections/" + cfg.getCollection();

        // 先删（不存在也无妨）
        try {
            restTemplate().exchange(url, HttpMethod.DELETE, new HttpEntity<>(jsonHeaders()), String.class);
        } catch (Exception e) {
            log.debug("[Qdrant] 删除 collection 忽略: {}", e.getMessage());
        }

        JsonObject vectors = new JsonObject();
        vectors.addProperty("size", ragProperties.getEmbedding().getDimension());
        vectors.addProperty("distance", cfg.getDistance());
        JsonObject body = new JsonObject();
        body.add("vectors", vectors);

        try {
            restTemplate().exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(GSON.toJson(body), jsonHeaders()), String.class);
            log.info("[Qdrant] 已重建 collection {}（dim={}, distance={}）",
                    cfg.getCollection(), ragProperties.getEmbedding().getDimension(), cfg.getDistance());
        } catch (Exception e) {
            throw new QdrantException("创建 collection 失败：" + e.getMessage(), e);
        }
    }

    /** 批量写入向量点。 */
    public void upsert(List<Point> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        RagProperties.Qdrant cfg = ragProperties.getQdrant();
        String url = baseUrl() + "/collections/" + cfg.getCollection() + "/points?wait=true";

        JsonArray arr = new JsonArray();
        for (Point p : points) {
            JsonObject point = new JsonObject();
            point.addProperty("id", p.getId());
            JsonArray vec = new JsonArray();
            for (float v : p.getVector()) {
                vec.add(v);
            }
            point.add("vector", vec);
            JsonObject payload = new JsonObject();
            payload.addProperty("content", p.getContent());
            payload.addProperty("source", p.getSource());
            payload.addProperty("title", p.getTitle());
            point.add("payload", payload);
            arr.add(point);
        }
        JsonObject body = new JsonObject();
        body.add("points", arr);

        try {
            restTemplate().exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(GSON.toJson(body), jsonHeaders()), String.class);
        } catch (Exception e) {
            throw new QdrantException("写入向量失败：" + e.getMessage(), e);
        }
    }

    /** 向量相似度检索，返回 Top-K 命中。 */
    public List<KnowledgeHit> search(float[] vector, int topK) {
        RagProperties.Qdrant cfg = ragProperties.getQdrant();
        String url = baseUrl() + "/collections/" + cfg.getCollection() + "/points/search";

        JsonArray vec = new JsonArray();
        for (float v : vector) {
            vec.add(v);
        }
        JsonObject body = new JsonObject();
        body.add("vector", vec);
        body.addProperty("limit", Math.max(1, topK));
        body.addProperty("with_payload", true);

        try {
            ResponseEntity<String> rsp = restTemplate().exchange(url, HttpMethod.POST,
                    new HttpEntity<>(GSON.toJson(body), jsonHeaders()), String.class);
            return parseHits(rsp.getBody());
        } catch (QdrantException e) {
            throw e;
        } catch (Exception e) {
            throw new QdrantException("向量检索失败：" + e.getMessage(), e);
        }
    }

    private List<KnowledgeHit> parseHits(String resp) {
        List<KnowledgeHit> hits = new ArrayList<>();
        if (resp == null || resp.isEmpty()) {
            return hits;
        }
        JsonObject json = GSON.fromJson(resp, JsonObject.class);
        if (json == null || !json.has("result") || !json.get("result").isJsonArray()) {
            return hits;
        }
        for (JsonElement el : json.getAsJsonArray("result")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject item = el.getAsJsonObject();
            double score = item.has("score") && !item.get("score").isJsonNull() ? item.get("score").getAsDouble() : 0.0;
            JsonObject payload = item.has("payload") && item.get("payload").isJsonObject()
                    ? item.getAsJsonObject("payload") : new JsonObject();
            hits.add(new KnowledgeHit(
                    optString(payload, "content"),
                    optString(payload, "source"),
                    optString(payload, "title"),
                    score));
        }
        return hits;
    }

    private String baseUrl() {
        RagProperties.Qdrant cfg = ragProperties.getQdrant();
        return "http://" + cfg.getHost() + ":" + cfg.getPort();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private static String optString(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return o.get(key).toString();
        }
    }

    private RestTemplate restTemplate() {
        RestTemplate rt = this.restTemplate;
        if (rt == null) {
            synchronized (this) {
                rt = this.restTemplate;
                if (rt == null) {
                    RagProperties.Qdrant cfg = ragProperties.getQdrant();
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

    /** 待写入的向量点。 */
    public static class Point {
        private final long id;
        private final float[] vector;
        private final String content;
        private final String source;
        private final String title;

        public Point(long id, float[] vector, String content, String source, String title) {
            this.id = id;
            this.vector = vector;
            this.content = content;
            this.source = source;
            this.title = title;
        }

        public long getId() {
            return id;
        }

        public float[] getVector() {
            return vector;
        }

        public String getContent() {
            return content;
        }

        public String getSource() {
            return source;
        }

        public String getTitle() {
            return title;
        }
    }

    /** Qdrant 调用异常 */
    public static class QdrantException extends RuntimeException {
        public QdrantException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
