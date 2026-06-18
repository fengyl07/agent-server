package uyun.eagle.agent.alertagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;
import uyun.eagle.agent.alertagent.common.Utils;

/**
 * @author yangfei
 * @date 2019/3/21
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"uyun.eagle.agent.alertagent", "uyun.whale","com.alibaba.metrics.*"})
public class StartApplication {

    public static void main(String[] args) {
        String workdir = System.getProperty("work.dir", System.getProperty("user.dir"));
        System.setProperty("work.dir", workdir);
        String ymlName = Utils.getName();
        log.warn("appCode: {}",ymlName);
        new SpringApplicationBuilder(StartApplication.class).main(StartApplication.class)
                .properties("spring.config.name:" + ymlName + "-main-base," + ymlName + "-main-config").build().run(args);
    }

    /**
     * 全局 RestTemplate，供 Agent 调用 Alert OpenAPI 及其他 HTTP 接入使用。
     */
    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

}