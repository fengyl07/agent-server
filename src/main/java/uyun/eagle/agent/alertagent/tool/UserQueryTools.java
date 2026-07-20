package uyun.eagle.agent.alertagent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.eagle.agent.alertagent.client.AlertOpenApiClient;
import uyun.eagle.agent.alertagent.tool.dto.UserBrief;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户查询 Tool（只读）。
 *
 * <p>按姓名/账号关键字查当前租户用户，主要服务于「转派」场景：把用户口语中的人名解析成 userId，
 * 供 LLM 回显候选、取得确认后再调用转派工具。
 *
 * <p>该工具为只读，可同时在 MCP 与 Chat 两条路径暴露。
 *
 * <p>注意：Alert 端 {@code /v2/user/query} 返回 ResultMessage 结构（{@code result}/{@code data}），
 * 与接手/备注/转派的 {@code statusCode} 判定不同，故不复用 {@code AlertActionTools} 的判定逻辑。
 */
@Slf4j
@Component
public class UserQueryTools {

    /** 默认返回候选条数上限 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    private AlertOpenApiClient alertOpenApiClient;

    /**
     * 按关键字查用户。
     *
     * @param args 工具参数，含 keyword（可空）、可选 limit
     * @return 用户摘要列表；无匹配时返回空列表（由 LLM 如实告知用户）
     */
    public List<UserBrief> findUser(JsonObject args) {
        JsonObject in = args == null ? new JsonObject() : args;
        String keyword = getString(in, "keyword");
        int limit = getInt(in, "limit", DEFAULT_PAGE_SIZE);

        JsonObject rsp = alertOpenApiClient.queryUsers(keyword, 1, limit);
        return parseUsers(rsp);
    }

    /** 解析 ResultMessage：result==true 时取 data 数组，逐项转 UserBrief。 */
    private static List<UserBrief> parseUsers(JsonObject rsp) {
        List<UserBrief> users = new ArrayList<>();
        if (rsp == null) {
            return users;
        }
        if (rsp.has("result") && !rsp.get("result").isJsonNull()) {
            try {
                if (!rsp.get("result").getAsBoolean()) {
                    return users;
                }
            } catch (Exception ignored) {
                // result 非布尔时继续尝试解析 data
            }
        }
        if (!rsp.has("data") || !rsp.get("data").isJsonArray()) {
            return users;
        }
        JsonArray data = rsp.getAsJsonArray("data");
        for (JsonElement el : data) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            UserBrief u = new UserBrief();
            u.setUserId(getString(o, "userId"));
            u.setRealName(getString(o, "realName"));
            u.setAccount(getString(o, "account"));
            if (u.getUserId() != null) {
                users.add(u);
            }
        }
        return users;
    }

    private static String getString(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return o.get(key).toString();
        }
    }

    private static int getInt(JsonObject o, String key, int defaultValue) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Exception e) {
            try {
                return Integer.parseInt(o.get(key).getAsString().trim());
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
    }
}
