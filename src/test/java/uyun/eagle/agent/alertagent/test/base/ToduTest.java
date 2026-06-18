package uyun.eagle.agent.alertagent.test.base;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class ToduTest extends BaseControllerTest {

    @Autowired
    RestTemplate restTemplate;

    private final String url ="http://127.0.0.1:8033/alertagent/openapi/v1/todos/query?apikey=ssssssssssssssss&dutyDate=2222222222&currentUser=三生三世";

    @Test
    public void instertToduTest() {
        log.warn("test..............");
        String res = restTemplate.getForObject(url,String.class);
        log.warn("res:"+res);
    }
}
