package uyun.eagle.agent.alertagent.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.config.KnowledgeProperties;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 运维知识库加载器（Phase 1c）。
 *
 * <p>启动时读取 {@link KnowledgeProperties#getLocation()} 匹配到的 Markdown 文档，按文件名排序后整体拼接，
 * 缓存为一段领域知识文本，供 LLM 编排时注入 system 消息。整份注入、不做向量检索（非 RAG）。
 *
 * <p>读取失败或文档为空不影响启动：知识文本为空，Agent 回退到仅基础约束的 system。
 */
@Slf4j
@Component
public class KnowledgeBaseLoader {

    @Autowired
    private KnowledgeProperties knowledgeProperties;

    /** 启动时加载完成的知识文本；为空字符串表示无可用知识 */
    private volatile String knowledgeText = "";

    @PostConstruct
    public void load() {
        if (!knowledgeProperties.isEnabled()) {
            log.info("[KnowledgeBase] 知识库注入已关闭（knowledge.enabled=false）");
            this.knowledgeText = "";
            return;
        }

        String location = knowledgeProperties.getLocation();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(location);
            List<Resource> sorted = new ArrayList<>();
            for (Resource r : resources) {
                if (r.isReadable()) {
                    sorted.add(r);
                }
            }
            sorted.sort(Comparator.comparing(r -> safeName(r)));

            StringBuilder sb = new StringBuilder();
            for (Resource r : sorted) {
                String content = readContent(r);
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(content.trim());
            }

            String text = sb.toString();
            int maxChars = knowledgeProperties.getMaxChars();
            if (maxChars > 0 && text.length() > maxChars) {
                log.warn("[KnowledgeBase] 知识文本长度 {} 超过上限 {}，已截断", text.length(), maxChars);
                text = text.substring(0, maxChars);
            }

            this.knowledgeText = text;
            log.info("[KnowledgeBase] 已加载 {} 个知识文档，合计 {} 字符（location={}）",
                    sorted.size(), text.length(), location);
        } catch (Exception e) {
            log.warn("[KnowledgeBase] 加载知识库失败，将不注入领域知识（location={}）: {}", location, e.getMessage());
            this.knowledgeText = "";
        }
    }

    /**
     * 返回拼接好的知识文本；无可用知识时返回空字符串（非 null）。
     */
    public String getKnowledgeText() {
        return knowledgeText == null ? "" : knowledgeText;
    }

    private static String safeName(Resource r) {
        String name = r.getFilename();
        return name == null ? "" : name;
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
            log.warn("[KnowledgeBase] 读取文档失败 {}: {}", safeName(r), e.getMessage());
            return null;
        }
    }
}
