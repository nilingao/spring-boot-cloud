package cn.com.nla.web.config.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 服务端的小程序白名单；默认关闭，不从请求读取密钥。 */
@Data
@Component
@ConfigurationProperties(prefix = "xcx")
public class XcxProperties {
    private boolean enabled;
    private Map<String, App> apps = new LinkedHashMap<>();

    @Data
    public static class App {
        private boolean enabled = true;
        @ToString.Exclude
        private String secret;
        /** 可登录/绑定此小程序的系统客户端 ID，未配置时拒绝。 */
        private List<String> clientIds = List.of();
    }
}
