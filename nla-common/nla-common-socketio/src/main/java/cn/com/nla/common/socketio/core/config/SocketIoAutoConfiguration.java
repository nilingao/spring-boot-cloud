package cn.com.nla.common.socketio.core.config;

import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import cn.com.nla.common.socketio.core.properties.SocketIoProperties;
import cn.com.nla.common.socketio.core.store.SocketIoRedissonStoreFactory;
import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** 默认关闭的 Boot 4 技术封装，无业务鉴权及端口扫描副作用。 @author TZY */
@AutoConfiguration
@ConditionalOnClass(SocketIOServer.class)
@ConditionalOnProperty(prefix = "socket-io", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SocketIoProperties.class)
@Import(SocketIoAutoConfiguration.RedisStoreConfiguration.class)
public class SocketIoAutoConfiguration {
    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(StoreFactory.class)
    @ConditionalOnProperty(prefix = "socket-io", name = "store", havingValue = "memory", matchIfMissing = true)
    public StoreFactory socketIoMemoryStoreFactory() {
        return new MemoryStoreFactory();
    }

    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean(SocketIOServer.class)
    public SocketIOServer socketIOServer(SocketIoProperties properties, StoreFactory storeFactory,
            ObjectProvider<AuthorizationListener> authorization,
            ObjectProvider<SocketIoConfigurationCustomizer> customizers) {
        properties.validate();
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        com.corundumstudio.socketio.SocketConfig socket = new com.corundumstudio.socketio.SocketConfig();
        socket.setReuseAddress(true);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(-1);
        config.setSocketConfig(socket);
        config.setHostname(properties.getHost());
        config.setPort(properties.getPort());
        config.setContext(properties.getContext());
        if (properties.getOrigin() != null) { config.setOrigin(properties.getOrigin()); }
        config.setBossThreads(properties.getBossCount());
        config.setWorkerThreads(properties.getWorkCount());
        config.setAllowCustomRequests(properties.getAllowCustomRequests());
        config.setUpgradeTimeout(properties.getUpgradeTimeout());
        config.setPingInterval(properties.getPingInterval());
        config.setPingTimeout(properties.getPingTimeout());
        config.setMaxFramePayloadLength(properties.getMaxFramePayloadLength());
        config.setMaxHttpContentLength(properties.getMaxHttpContentLength());
        config.setStoreFactory(storeFactory);
        AuthorizationListener listener = authorization.getIfAvailable();
        if (listener != null) { config.setAuthorizationListener(listener); }
        customizers.orderedStream().forEach(customizer -> customizer.customize(config));
        return new SocketIOServer(config);
    }

    @Bean
    @ConditionalOnMissingBean(SocketIoListenerRegistrar.class)
    public static SocketIoListenerRegistrar socketIoListenerRegistrar(ObjectProvider<SocketIOServer> server,
            ObjectProvider<NamespaceListener> namespaces, ObjectProvider<EventListener<?>> events) {
        return new SocketIoListenerRegistrar(server, namespaces, events);
    }

    @Bean
    @ConditionalOnMissingBean(SocketIoLifecycle.class)
    public SocketIoLifecycle socketIoLifecycle(SocketIOServer server, SocketIoProperties properties) {
        return new SocketIoLifecycle(server, properties.isAutoStart());
    }

    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnProperty(prefix = "socket-io", name = "store", havingValue = "redisson")
    static class RedisStoreConfiguration {
        @Bean(destroyMethod = "shutdown")
        @ConditionalOnMissingBean(StoreFactory.class)
        SocketIoRedissonStoreFactory socketIoRedissonStoreFactory(RedissonClient client, SocketIoProperties properties) {
            properties.validate();
            return new SocketIoRedissonStoreFactory(client, properties.getRedisPrefix());
        }
    }
}
