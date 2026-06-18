package uyun.eagle.agent.alertagent.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uyun.whale.consumer.common.ApiSupport;
import uyun.whale.consumer.common.WebContext;
import uyun.whale.consumer.config.AppProperties;
import uyun.tenant.serviceapi.dto.TenantDTO;
import uyun.whale.i18n.api.I18nContext;
import uyun.whale.i18n.impls.I18nService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: yangfei
 * @Date: 2019/4/08 Time: 15:52
 * @desc 国际化信息获取
 */
@Slf4j
@Component
public class LocaleMessage {

    private static final String ZH_CN = "zh_CN";

    public static Map<String, String> cache = new ConcurrentHashMap<String, String>();

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ApiSupport apiSupport;

    /**
     * @param code：对应文本配置的key.
     * @return 对应地区的语言消息字符串
     */
    public String getMessage(String code) {
        return this.getMessage(code, null);
    }

    public String getMessage(String code, Object[] args) {
        log.debug("code {}", code);
        return I18nService.getMessage(getLang(), code, args);
    }

    public String getLang() {
        String tenantId = WebContext.getUser().getTenantId();
        log.debug("tenantId: {}", tenantId);
        // 获取当前语言
        String lang = getLang(tenantId);
        if (lang == null) {
            // 获取抛出异常
            lang = I18nContext.getLang();
        }
        return lang;
    }

    private String getLang(String tenantId) {
        String lang = cache.get(tenantId);
        if (lang != null) {
            return lang;
        }
        // 尝试从线程上下文获取，见 ApiSupport
        lang = WebContext.getUser().getLanguage();
        if (lang == null) {
            TenantDTO tenant = apiSupport.getTenantService(tenantId);
            if (tenant != null) {
                lang = tenant.getLanguage();
            }
        }
        if (lang == null) {
            return ZH_CN;
        } else {
            cache.put(tenantId, lang);
        }
        return lang;
    }

}