package uyun.eagle.agent.alertagent.knowledge.index;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.config.RagProperties;
import uyun.eagle.agent.alertagent.knowledge.embedding.EmbeddingClient;
import uyun.eagle.agent.alertagent.knowledge.vector.QdrantVectorStore;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库索引器（Phase 1d）。
 *
 * <p>启动时（{@code rag.enabled=true && rag.index.auto-on-startup=true}）把 {@code rag.index.location}
 * 匹配到的 Markdown 切块、向量化后全量重建到 Qdrant。为避免外部 Embedding/Qdrant 抖动拖慢或阻断启动，
 * 索引在独立守护线程执行，任何异常仅告警、不影响应用启动与其它功能。
 */
@Slf4j
@Component
public class KnowledgeIndexer {

    /** Qdrant 单次写入的点数上限，避免单请求过大 */
    private static final int UPSERT_BATCH = 64;

    @Autowired
    private RagProperties ragProperties;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private QdrantVectorStore qdrantVectorStore;

    @PostConstruct
    public void init() {
        if (!ragProperties.isEnabled()) {
            log.info("[RAG] 未启用（rag.enabled=false），跳过知识索引");
            return;
        }
        if (!ragProperties.getIndex().isAutoOnStartup()) {
            log.info("[RAG] 已关闭启动自动索引（rag.index.auto-on-startup=false）");
            return;
        }
        Thread t = new Thread(this::rebuildQuietly, "rag-knowledge-indexer");
        t.setDaemon(true);
        t.start();
    }

    private void rebuildQuietly() {
        try {
            rebuild();
        } catch (Exception e) {
            log.warn("[RAG] 知识索引构建失败（不影响应用启动）：{}", e.getMessage());
        }
    }

    /**
     * 全量重建索引：读取 Markdown → 切块 → 向量化 → 重建 collection → 写入。
     *
     * @return 写入的片段数量
     */
    public int rebuild() {
        RagProperties.Index idx = ragProperties.getIndex();
        long t0 = System.currentTimeMillis();

        List<DocumentChunker.Chunk> chunks = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        loadChunks(idx, chunks, sources);
        if (chunks.isEmpty()) {
            log.warn("[RAG] 未读取到任何知识片段（location={}），跳过索引", idx.getLocation());
            return 0;
        }

        List<String> texts = new ArrayList<>(chunks.size());
        for (DocumentChunker.Chunk c : chunks) {
            texts.add(c.getContent());
        }
        List<float[]> vectors = embeddingClient.embedBatch(texts);
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("向量数量(" + vectors.size() + ")与片段数量(" + chunks.size() + ")不一致");
        }

        qdrantVectorStore.recreateCollection();

        List<QdrantVectorStore.Point> buffer = new ArrayList<>(UPSERT_BATCH);
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunker.Chunk c = chunks.get(i);
            buffer.add(new QdrantVectorStore.Point(i, vectors.get(i), c.getContent(), sources.get(i), c.getTitle()));
            if (buffer.size() >= UPSERT_BATCH) {
                qdrantVectorStore.upsert(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            qdrantVectorStore.upsert(buffer);
        }

        long cost = System.currentTimeMillis() - t0;
        log.info("[RAG] 知识索引完成：{} 个片段已写入 Qdrant collection {}，耗时 {} ms",
                chunks.size(), ragProperties.getQdrant().getCollection(), cost);
        return chunks.size();
    }

    private void loadChunks(RagProperties.Index idx, List<DocumentChunker.Chunk> outChunks, List<String> outSources) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(idx.getLocation());
            for (Resource r : resources) {
                if (!r.isReadable()) {
                    continue;
                }
                String content = readContent(r);
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                String source = r.getFilename() == null ? "unknown" : r.getFilename();
                List<DocumentChunker.Chunk> cs = DocumentChunker.chunk(content, idx.getChunkSize(), idx.getChunkOverlap());
                for (DocumentChunker.Chunk c : cs) {
                    outChunks.add(c);
                    outSources.add(source);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取知识文档失败：" + e.getMessage(), e);
        }
    }

    private static String readContent(Resource r) {
        try (InputStream in = r.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[RAG] 读取文档失败 {}: {}", r.getFilename(), e.getMessage());
            return null;
        }
    }
}
