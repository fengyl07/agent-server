package uyun.eagle.agent.alertagent.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

    /**
     * 告警接手（写操作）。
     *
     * <p>对应 Alert 端 {@code POST {OPEN_API}v2/incident/receiveByIncidentId}，完整路径
     * {baseUrl}/v2/incident/receiveByIncidentId。apikey 作为 query 参数传递，请求体为
     * {@code {"incidentId":"..."}}。接手人取自 apikey 对应的用户（Alert 端 getByApikey），
     * 无需也不能由调用方指定。
     *
     * <p>注意：该接口返回体结构与维护期不同——成功/失败以 {@code statusCode}（200/500）表示，
     * 无 {@code result}/{@code errCode} 字段，判定见 {@code AlertActionTools}。
     *
     * @param incidentId 告警 ID
     * @return Alert 返回的原始 JSON（含 statusCode/message）
     */
    public JsonObject receiveIncident(String incidentId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/receiveByIncidentId")
                .queryParam("apikey", alertProperties.getApikey())
                .build(true)
                .toUriString();
        JsonObject body = new JsonObject();
        body.addProperty("incidentId", incidentId);
        return postForJson(url, GSON.toJson(body), "告警接手");
    }

    /**
     * 告警备注（写操作）。
     *
     * <p>对应 Alert 端 {@code POST {OPEN_API}v2/incident/remarkByIncidentId}。apikey 走 query，
     * 请求体 {@code {"incidentId":"...","remark":"..."}}。备注人取自 apikey 对应用户。
     *
     * <p>返回体同接手：以 {@code statusCode}（200/500）表示成败，判定见 {@code AlertActionTools}。
     *
     * @param incidentId 告警 ID
     * @param remark     备注内容
     * @return Alert 返回的原始 JSON（含 statusCode/message）
     */
    public JsonObject remarkIncident(String incidentId, String remark) {
        String url = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/remarkByIncidentId")
                .queryParam("apikey", alertProperties.getApikey())
                .build(true)
                .toUriString();
        JsonObject body = new JsonObject();
        body.addProperty("incidentId", incidentId);
        body.addProperty("remark", remark);
        return postForJson(url, GSON.toJson(body), "告警备注");
    }

    /**
     * 告警转派（写操作）。
     *
     * <p>对应 Alert 端 {@code POST {OPEN_API}v2/incident/shiftByIncidentId}。apikey 走 query，
     * 请求体 {@code {"incidentId":"...","toUserId":"..."}}。当前 apikey 用户须为该告警负责人或管理员，
     * 否则 Alert 端返回失败（非本人告警不可操作）。
     *
     * <p>返回体同接手：以 {@code statusCode}（200/500）表示成败，判定见 {@code AlertActionTools}。
     *
     * @param incidentId 告警 ID
     * @param toUserId   转派目标用户 ID
     * @return Alert 返回的原始 JSON（含 statusCode/message）
     */
    public JsonObject transferIncident(String incidentId, String toUserId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/shiftByIncidentId")
                .queryParam("apikey", alertProperties.getApikey())
                .build(true)
                .toUriString();
        JsonObject body = new JsonObject();
        body.addProperty("incidentId", incidentId);
        body.addProperty("toUserId", toUserId);
        return postForJson(url, GSON.toJson(body), "告警转派");
    }

    /**
     * 告警关闭（写操作，不可逆终态）。
     *
     * <p>对应 Alert 端 {@code POST {OPEN_API}v2/incident/closeIncidentByIncidentId}。apikey 走 query，
     * 请求体 {@code {"incidentId":"...","closeMessage":"..."}}（对应后端 IncidentCloseParam）。
     * Alert 端会校验：告警已处于「已关闭」状态时返回失败。
     *
     * <p>返回体同接手：以 {@code statusCode}（200/500）表示成败，判定见 {@code AlertActionTools}。
     *
     * @param incidentId   告警 ID
     * @param closeMessage 关闭说明（必填）
     * @return Alert 返回的原始 JSON（含 statusCode/message）
     */
    public JsonObject closeIncident(String incidentId, String closeMessage) {
        String url = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/closeIncidentByIncidentId")
                .queryParam("apikey", alertProperties.getApikey())
                .build(true)
                .toUriString();
        JsonObject body = new JsonObject();
        body.addProperty("incidentId", incidentId);
        body.addProperty("closeMessage", closeMessage);
        return postForJson(url, GSON.toJson(body), "告警关闭");
    }

    /**
     * 告警解决（写操作，不可逆终态）。
     *
     * <p>对应 Alert 端 {@code POST {OPEN_API}v2/incident/resolveByIncidentId}。apikey 走 query，
     * 请求体 {@code {"incidentId":"...","resolveMessage":"..."}}。解决人取自 apikey 对应用户；
     * Alert 端有状态机校验，未接手/状态不允许时返回失败。
     *
     * <p>返回体同接手：以 {@code statusCode}（200/500）表示成败，判定见 {@code AlertActionTools}。
     *
     * @param incidentId     告警 ID
     * @param resolveMessage 解决说明（必填）
     * @return Alert 返回的原始 JSON（含 statusCode/message）
     */
    public JsonObject resolveIncident(String incidentId, String resolveMessage) {
        String url = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/resolveByIncidentId")
                .queryParam("apikey", alertProperties.getApikey())
                .build(true)
                .toUriString();
        JsonObject body = new JsonObject();
        body.addProperty("incidentId", incidentId);
        body.addProperty("resolveMessage", resolveMessage);
        return postForJson(url, GSON.toJson(body), "告警解决");
    }

    /**
     * 查询用户（只读）。
     *
     * <p>对应 Alert 端 {@code GET {OPEN_API}v2/user/query}，按姓名/账号关键字模糊查当前租户用户，
     * 供转派前把「人名」解析成 userId。apikey 走 query。
     *
     * <p>返回体为 ResultMessage 结构（{@code result}/{@code data}），与接手/备注不同，
     * 解析见 {@code UserQueryTools}。
     *
     * @param keyword  姓名/账号关键字（可空，空则返回租户用户列表）
     * @param pageNo   页码（从 1 开始）
     * @param pageSize 每页条数
     * @return Alert 返回的原始 JSON（含 result/data）
     */
    public JsonObject queryUsers(String keyword, int pageNo, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/user/query")
                .queryParam("apikey", alertProperties.getApikey())
                .queryParam("pageNo", pageNo <= 0 ? 1 : pageNo)
                .queryParam("pageSize", pageSize <= 0 ? 20 : pageSize);
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.queryParam("keyword", encode(keyword.trim()));
        }
        return getForJson(builder.build(true).toUriString(), "用户查询");
    }

    /**
     * 告警分组统计（只读）。
     *
     * <p>对应 Alert 端 {@code GET {OPEN_API}v2/incident/statistics}，按 {@code groupBy} 指定维度分组计数。
     * apikey 走 query。返回体为 JSON 数组 {@code [{"value":"...","count":N}, ...]}，
     * 其中 value 为分组值（severity/status 维度为数字码），count 为该组告警数。
     *
     * @param groupBy 分组维度字段：severity/status/source/classCode/tags/hour/dayOfMonth/week 等
     * @param begin   起始时间（毫秒时间戳，可空表示不限）
     * @param end     结束时间（毫秒时间戳，可空表示不限）
     * @param top     取前 N 组（可空或 &lt;=0 时不下传）
     * @param sort    排序方向：DESC/ASC（可空）
     * @return Alert 返回的原始 JSON 数组（每项含 value/count）
     */
    public JsonArray statistics(String groupBy, Long begin, Long end, Integer top, String sort) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(alertProperties.getBaseUrl() + "/v2/incident/statistics")
                .queryParam("apikey", alertProperties.getApikey())
                .queryParam("groupBy", groupBy);
        if (begin != null) {
            builder.queryParam("begin", begin);
        }
        if (end != null) {
            builder.queryParam("end", end);
        }
        if (top != null && top > 0) {
            builder.queryParam("top", top);
        }
        if (sort != null && !sort.trim().isEmpty()) {
            builder.queryParam("sort", sort.trim());
        }
        return getForJsonArray(builder.build(true).toUriString(), "告警分组统计");
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

    private JsonArray getForJsonArray(String url, String action) {
        try {
            log.info("[AlertOpenApi] {} -> {}", action, maskApikey(url));
            String rsp = restTemplate.getForObject(url, String.class);
            if (rsp == null || rsp.isEmpty()) {
                return new JsonArray();
            }
            return GSON.fromJson(rsp, JsonArray.class);
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
