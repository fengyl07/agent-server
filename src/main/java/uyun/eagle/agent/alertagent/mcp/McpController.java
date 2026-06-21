package uyun.eagle.agent.alertagent.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP Server 单端点（Phase 1，Streamable HTTP 简化版）。
 *
 * <p>实际地址：{@code POST /alertagent/mcp}，接收 JSON-RPC 2.0 消息（单条或批量），
 * 返回 {@code application/json} 响应。仅暴露 {@link McpToolRegistry} 的 4 个只读查询工具。
 *
 * <p>该路径需在 base 配置的 ineffectiveness-path 中放行（测试期免登录）。
 * 通知类消息（无 id）无响应体时返回 202。
 */
@Slf4j
@RestController
@RequestMapping("/{appcode}/mcp")
public class McpController {

    private static final Gson GSON = new Gson();

    @Autowired
    private McpProperties mcpProperties;

    @Autowired
    private McpProtocolHandler protocolHandler;

    @PostMapping(consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handle(@RequestBody(required = false) String body) {
        if (!mcpProperties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson("MCP 已禁用"));
        }
        if (body == null || body.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorJson("请求体为空"));
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (Exception e) {
            log.warn("[MCP] JSON 解析失败: {}", e.getMessage());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(parseError());
        }

        // 批量请求
        if (root.isJsonArray()) {
            JsonArray responses = new JsonArray();
            for (JsonElement el : root.getAsJsonArray()) {
                if (el.isJsonObject()) {
                    JsonObject rsp = protocolHandler.handle(el.getAsJsonObject());
                    if (rsp != null) {
                        responses.add(rsp);
                    }
                }
            }
            if (responses.size() == 0) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GSON.toJson(responses));
        }

        // 单条请求
        if (root.isJsonObject()) {
            JsonObject rsp = protocolHandler.handle(root.getAsJsonObject());
            if (rsp == null) {
                return ResponseEntity.accepted().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GSON.toJson(rsp));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(parseError());
    }

    private static String errorJson(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("error", message);
        return GSON.toJson(o);
    }

    private static String parseError() {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.add("id", com.google.gson.JsonNull.INSTANCE);
        JsonObject err = new JsonObject();
        err.addProperty("code", -32700);
        err.addProperty("message", "解析错误：非法 JSON");
        o.add("error", err);
        return GSON.toJson(o);
    }
}
