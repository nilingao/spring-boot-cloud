package cn.com.nla.common.mq.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** NLA 封装开关；连接和监听配置仍由消费方的 Boot AMQP starter 管理。 @author TZY */
@ConfigurationProperties("nla.mq")
public class MqProperties {
    private boolean enabled;
    private boolean autoDeclare = true;
    private String[] trustedPackages = {"cn.com.nla"};
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isAutoDeclare() { return autoDeclare; }
    public void setAutoDeclare(boolean autoDeclare) { this.autoDeclare = autoDeclare; }
    public String[] getTrustedPackages() { return trustedPackages.clone(); }
    public void setTrustedPackages(String[] trustedPackages) {
        if (trustedPackages == null || trustedPackages.length == 0
            || java.util.Arrays.stream(trustedPackages).anyMatch(p -> p == null || p.isBlank())) {
            throw new IllegalArgumentException("nla.mq.trusted-packages must contain at least one package");
        }
        this.trustedPackages = trustedPackages.clone();
    }
}
