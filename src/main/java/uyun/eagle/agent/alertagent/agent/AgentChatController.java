package uyun.eagle.agent.alertagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uyun.eagle.agent.alertagent.agent.dto.AgentChatRequest;
import uyun.eagle.agent.alertagent.agent.dto.AgentChatResponse;

/**
 * 告警 Agent 对话入口（Phase 1，供 Postman / 前端 / 未来 ChatOps 调用）。
 *
 * <p>路径与脚手架 frontapi 规约一致：实际地址为 {@code /alertagent/frontapi/v1/agent/chat}。
 * 手写 Controller（不走 swagger-codegen 生成），避免构建期代码生成耦合。
 * 该路径已在 base 配置的 ineffectiveness-path 中放行，测试期无需登录。
 */
@Slf4j
@RestController
@RequestMapping("/{appcode}/frontapi/v1/agent")
public class AgentChatController {

    @Autowired
    private AlertAgentService alertAgentService;

    @PostMapping("/chat")
    public AgentChatResponse chat(@RequestBody AgentChatRequest request) {
        String message = request == null ? null : request.getMessage();
        String sessionId = request == null ? null : request.getSessionId();
        log.info("[AgentChat] sessionId={}, message={}", sessionId, message);
        return alertAgentService.chat(message, sessionId);
    }
}
