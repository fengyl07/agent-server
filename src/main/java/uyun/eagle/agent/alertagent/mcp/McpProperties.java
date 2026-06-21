package uyun.eagle.agent.alertagent.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCP Server 配置（Phase 1）。
 *
 * <p>对应配置前缀 {@code alert.mcp.*}，用于控制 MCP 端点开关与 serverInfo 元数据。
 * 默认协议版本与 MCP 规范保持一致；客户端在 initialize 时若声明了自己的版本，会优先回显客户端版本。
 */
@Data
@Component
@ConfigurationProperties(prefix = "alert.mcp")
public class McpProperties {

    /** 是否启用 MCP 端点 */
    private boolean enabled = true;

    /** serverInfo.name */
    private String serverName = "alert-agent";

    /** serverInfo.version */
    private String serverVersion = "1.0.0";

    /** 默认 MCP 协议版本（客户端未声明时使用） */
    private String protocolVersion = "2024-11-05";
}
