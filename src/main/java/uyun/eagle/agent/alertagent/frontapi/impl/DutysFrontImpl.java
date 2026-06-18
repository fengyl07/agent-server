package uyun.eagle.agent.alertagent.frontapi.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import uyun.eagle.agent.alertagent.exception.DemoException;
import uyun.eagle.agent.alertagent.frontapi.DutysFrontApi;
import uyun.eagle.agent.alertagent.frontapi.vo.DutyVO;
import uyun.whale.consumer.common.WebContext;
import uyun.whale.consumer.config.AppProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: yangfei
 * @Date: 2019/4/08 Time: 15:52
 * @desc 获取值班信息, 通过rota获取
 */

@Slf4j
@RestController
public class DutysFrontImpl implements DutysFrontApi {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public List<DutyVO> queryDutys(Long dutyDate) {
        List<DutyVO> dutys = new ArrayList<DutyVO>();

        String apikey = WebContext.getUser().getApiKeys().get(0).getKey();
        String url = appProperties.getBaseurl() + "rota/openapi/v1/date/schedule/list?date=" + dutyDate + "&apikey=" + apikey;
        try {
            String rsp = restTemplate.getForObject(url, String.class);
            log.debug("url: {} rsp{}", url, rsp);
            JsonObject json = new Gson().fromJson(rsp, JsonObject.class);
            if (json.getAsJsonArray("data").size() != 0) {
                json.getAsJsonArray("data").get(0).getAsJsonObject().getAsJsonArray("dailySchedules").forEach(e -> {
                    DutyVO vo = new DutyVO();
                    vo.setShiftName(e.getAsJsonObject().get("shiftName").getAsString());
                    e.getAsJsonObject().getAsJsonArray("users").forEach(u -> {
                        if (vo.getUsers() == null) {
                            vo.setUsers(new ArrayList<>());
                        }
                        vo.getUsers().add(u.getAsJsonObject().get("name").getAsString());
                    });

                    dutys.add(vo);
                });
            }
        } catch (Exception e) {
            throw DemoException.createClientException(String.valueOf(HttpStatus.NOT_FOUND.value()), "获取值班数据失败，原因：" + e.getMessage());
        }

        return dutys;
    }

}
