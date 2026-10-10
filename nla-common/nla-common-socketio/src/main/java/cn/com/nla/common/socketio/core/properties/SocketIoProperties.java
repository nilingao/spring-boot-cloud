package cn.com.nla.common.socketio.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Socket.IO 配置；时间单位均为毫秒。 @author TZY */
@Data
@ConfigurationProperties("socket-io")
public class SocketIoProperties {
    private boolean enabled;
    private boolean autoStart = true;
    private String name = "socket-service";
    private String host = "0.0.0.0";
    private int port = 9092;
    private String context = "/socket.io";
    private String origin;
    private int bossCount = 1;
    private int workCount = 0;
    private int maxFramePayloadLength = 1024 * 1024;
    private int maxHttpContentLength = 1024 * 1024;
    private boolean allowCustomRequests;
    private int upgradeTimeout = 10_000;
    private int pingTimeout = 30_000;
    private int pingInterval = 12_000;
    private Store store = Store.MEMORY;
    private String redisPrefix = "nla:socketio";

    public enum Store { MEMORY, REDISSON }

    public boolean getAllowCustomRequests() { return allowCustomRequests; }

    public void validate() {
        if (host == null || host.isBlank() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("socket-io.host/port must specify a valid listening address");
        }
        if (context == null || !context.startsWith("/") || context.endsWith("/") || context.contains("?")) {
            throw new IllegalArgumentException("socket-io.context must start with / and have no trailing slash or query");
        }
        if (bossCount < 1 || workCount < 0 || maxFramePayloadLength < 1 || maxHttpContentLength < 1
            || upgradeTimeout < 1 || pingTimeout < 1 || pingInterval < 1) {
            throw new IllegalArgumentException("socket-io thread counts, payload limits and timeouts must be valid");
        }
        if (store == null || redisPrefix == null || redisPrefix.isBlank()) {
            throw new IllegalArgumentException("socket-io.store/redis-prefix must not be empty");
        }
    }
}
