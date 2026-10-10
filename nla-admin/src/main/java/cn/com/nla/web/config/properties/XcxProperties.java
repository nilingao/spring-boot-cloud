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
    /** 二维码网页登录独立开关，默认关闭。 */
    private boolean qrEnabled;
    private Map<String, App> apps = new LinkedHashMap<>();

    @Data
    public static class App {
        private boolean enabled = true;
        @ToString.Exclude
        private String secret;
        /** 可登录/绑定此小程序的系统客户端 ID，未配置时拒绝。 */
        private List<String> clientIds = List.of();
        /** 允许通过此小程序扫码登录的网页客户端（必须另启用 qr grant）。 */
        private List<String> qrClientIds = List.of();
        private String qrPage;
        private String qrEnvVersion = "release";
    }
}
