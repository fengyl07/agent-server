package uyun.eagle.agent.alertagent.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * 对应 Alert 的 OpenAPI（baseUrl = http://host:port/alert/openapi，不含 /v2）：
 * <ul>
 *     <li>GET /v2/incident/query —— 告警列表查询（含 total）</li>
 *     <li>GET AlertQueryTools/v2/incident/getIncidentById —— 单条告警详情</li>
 * </ul>
 * 注意：Alert 端 Controller 映射为 {OPEN_API}v2/incident，故完整路径为 {baseUrl}/v2/incident/xxx，
 * baseUrl 末尾不要再带 /v2，否则会出现 /v2/v2 的 404。
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
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/getIncidentById")
                .queryParam("apikey", alertProperties.getApikey())
                .queryParam("incidentId", incidentId)
                .build(true)
                .toUriString();
        return getForJson(url, "告警详情查询");
    }

    /**
     * 创建维护期（写操作）。
     *
     * <p>对应 Alert 端 {@code POST {OPEN_API}v2/maintenance/create}，完整路径 {baseUrl}/v2/maintenance/create。
     * apikey 作为 query 参数传递（Alert 端 {@code MaintenanceOpenController} 通过 request.getParameter 读取，
     * 不读 header），请求体为 {@code MaintainCreateParam} 的 JSON。
     *
     * @param bodyJson MaintainCreateParam 的 JSON 字符串
     * @return Alert 返回的原始 JSON（ResultMessage 结构：result/message/data/errCode）
     */
    public JsonObject createMaintenance(String bodyJson) {
        String url = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/maintenance/create")
                .queryParam("apikey", alertProperties.getApikey())
                .build(true)
                .toUriString();
        return postForJson(url, bodyJson, "维护期创建");
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

    private JsonObject postForJson(String url, String bodyJson, String action) {
        try {
            log.info("[AlertOpenApi] {} -> {}", action, maskApikey(url));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
            String rsp = restTemplate.postForObject(url, entity, String.class);
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
