package uyun.eagle.agent.alertagent.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uyun.eagle.agent.alertagent.config.AlertProperties;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Alert OpenAPI 调用封装（Phase 1，只读）。
 *
 * <p>统一处理 baseUrl、apikey、错误与 JSON 解析；不依赖 alert-agent 本地数据库。
 * 对应 Alert 的 OpenAPI（basePath = /alert/openapi/v2）：
 * <ul>
 *     <li>GET /v2/incident/query —— 告警列表查询（含 total）</li>
 *     <li>GET /incident/getIncidentById —— 单条告警详情</li>
 * </ul>
 * 注意：query 接口在 Alert 端的完整路径为 {baseUrl}/v2/incident/query（baseUrl 已含一层 /alert/openapi/v2）。
 */
@Slf4j
@Component
public class AlertOpenApiClient {

    private static final Gson GSON = new Gson();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AlertProperties alertProperties;

    /**
     * 告警列表查询。
     *
     * @param params 查询参数（不含 apikey），如 pageNo、pageSize、severity、status、entityName、begin、end 等
     * @return Alert 返回的原始 JSON（含 pageNo/pageSize/total/records）
     */
    public JsonObject queryAlerts(Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/query")
                .queryParam("apikey", alertProperties.getApikey());
        if (params != null) {
            params.forEach((k, v) -> {
                if (v != null && !v.isEmpty()) {
                    builder.queryParam(k, encode(v));
                }
            });
        }
        return getForJson(builder.build(true).toUriString(), "告警列表查询");
    }

    /**
     * 根据告警 ID 查询详情。
     *
     * @param incidentId 告警 ID
     * @return Alert 返回的原始 JSON（告警详情字段）
     */
    public JsonObject getAlertById(String incidentId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/incident/getIncidentById")
                .queryParam("apikey", alertProperties.getApikey())
                .queryParam("incidentId", incidentId)
                .build(true)
                .toUriString();
        return getForJson(url, "告警详情查询");
    }

    private JsonObject getForJson(String url, String action) {
        try {
            log.info("[AlertOpenApi] {} -> {}", action, maskApikey(url));
            String rsp = restTemplate.getForObject(url, String.class);
            if (rsp == null || rsp.isEmpty()) {
                return new JsonObject();
            }
            return GSON.fromJson(rsp, JsonObject.class);
        } catch (Exception e) {
            log.error("[AlertOpenApi] {} 失败: {}", action, e.getMessage());
            throw new AlertApiException(action + "失败：" + e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /** 日志脱敏：隐藏 apikey 取值 */
    private static String maskApikey(String url) {
        return url.replaceAll("(apikey=)[^&]*", "$1******");
    }

    /** Alert OpenAPI 调用异常 */
    public static class AlertApiException extends RuntimeException {
        public AlertApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
