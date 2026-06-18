package uyun.eagle.agent.alertagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Alert OpenAPI 接入配置。
 *
 * <p>base-url 指向测试环境 Alert 网关的 OpenAPI 基路径，例如
 * {@code http://alert-gateway-dev/alert/openapi/v2}；apikey 走 Apollo 注入，禁止硬编码到代码或日志。
 */
@Data
@Component
@ConfigurationProperties(prefix = "alert.openapi")
public class AlertProperties {

    /** Alert OpenAPI 基路径，结尾不带斜杠，如 http://alert-gateway-dev/alert/openapi/v2 */
    private String baseUrl;

    /** 访问 Alert OpenAPI 的 apikey（敏感，Apollo 注入） */
    private String apikey;

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 30000;
}
