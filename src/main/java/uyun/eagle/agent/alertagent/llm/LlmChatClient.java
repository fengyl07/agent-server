package uyun.eagle.agent.alertagent.llm;

import com.google.gson.Gson;
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
import uyun.eagle.agent.alertagent.config.LlmProperties;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * LLM 调用封装（Phase 1b，OpenAI 兼容 Chat Completions）。
 *
 * <p>对接 DeepSeek API / Ollama / 公司模型网关，端点为 {@code {baseUrl}/chat/completions}。
 * 自建带长超时的 {@link RestTemplate}（LLM 生成较慢），不复用平台共享 bean，避免超时不足与 bean 冲突。
 * apikey 仅放入 Authorization 头，日志不输出。
 */
@Slf4j
@Component
public class LlmChatClient {

    private static final Gson GSON = new Gson();

    @Autowired
    private LlmProperties llmProperties;

    private volatile RestTemplate restTemplate;

    /**
     * 发起一次 Chat Completions 调用。
     *
     * @param requestBody 已构造好的请求体（含 model / messages / tools 等）
     * @return 响应 JSON（OpenAI 兼容，含 choices）
     * @throws LlmException 调用失败或响应异常
     */
    public JsonObject chatCompletion(JsonObject requestBody) {
        String url = endpoint();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (llmProperties.getApiKey() != null && !llmProperties.getApiKey().trim().isEmpty()) {
            headers.setBearerAuth(llmProperties.getApiKey().trim());
        }

        HttpEntity<String> entity = new HttpEntity<>(GSON.toJson(requestBody), headers);
        try {
            log.info("[LLM] 调用模型 {} -> {}", llmProperties.getModel(), url);
            ResponseEntity<String> rsp = restTemplate().postForEntity(url, entity, String.class);
            String body = rsp.getBody();
            if (body == null || body.isEmpty()) {
                throw new LlmException("LLM 返回为空");
            }
            return GSON.fromJson(body, JsonObject.class);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("[LLM] 调用失败: {}", e.getMessage());
            throw new LlmException("LLM 调用失败：" + e.getMessage(), e);
        }
    }

    private String endpoint() {
        String base = llmProperties.getBaseUrl();
        if (base == null || base.trim().isEmpty()) {
            throw new LlmException("未配置 llm.base-url");
        }
        base = base.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/chat/completions";
    }

    private RestTemplate restTemplate() {
        RestTemplate rt = this.restTemplate;
        if (rt == null) {
            synchronized (this) {
                rt = this.restTemplate;
                if (rt == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(llmProperties.getConnectTimeout());
                    factory.setReadTimeout(llmProperties.getReadTimeout());
                    rt = new RestTemplate(factory);
                    // 确保以 UTF-8 处理中文，避免乱码
                    rt.getMessageConverters().add(0,
                            new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8));
                    this.restTemplate = rt;
                }
            }
        }
        return rt;
    }

    /** LLM 调用异常 */
    public static class LlmException extends RuntimeException {
        public LlmException(String message) {
            super(message);
        }

        public LlmException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
