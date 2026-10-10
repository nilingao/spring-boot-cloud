package cn.com.nla.common.socketio.core.config;

import com.corundumstudio.socketio.Configuration;

/** 按 Spring 顺序定制原生配置，可设置 TLS、transport 或异常处理器。 @author TZY */
@FunctionalInterface
public interface SocketIoConfigurationCustomizer {
    void customize(Configuration configuration);
}
