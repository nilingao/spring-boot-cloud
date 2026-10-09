package cn.com.nla.common.freeswitch.esl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * FreeSWITCH ESL 入站客户端配置。
 * <p>
 * 替代 freeswitch-esl-spring-boot-starter 的 {@code InboundClientProperties},
 * 沿用相同配置前缀 {@code link.thingscloud.freeswitch.esl.inbound} 与默认值, 保证既有 YAML 无需改动。
 *
 * @author TZY
 */
@Data
@ConfigurationProperties(prefix = "link.thingscloud.freeswitch.esl.inbound")
public class FreeswitchEslProperties {

    /**
     * 发送缓冲区大小
     */
    private int sndBufSize = 65535;
    /**
     * 接收缓冲区大小
     */
    private int rcvBufSize = 65535;
    /**
     * Netty worker 线程数, 默认 CPU 核数 * 2
     */
    private int workerGroupThread = Runtime.getRuntime().availableProcessors() * 2;
    /**
     * 公共执行器线程数, 默认 CPU 核数 * 2
     */
    private int publicExecutorThread = Runtime.getRuntime().availableProcessors() * 2;
    /**
     * 私有执行器线程数, 默认 CPU 核数 * 2
     */
    private int privateExecutorThread = Runtime.getRuntime().availableProcessors() * 2;
    /**
     * 回调执行器线程数, 默认 CPU 核数 * 2
     */
    private int callbackExecutorThread = Runtime.getRuntime().availableProcessors() * 2;
    /**
     * 默认命令超时时间(秒)
     */
    private int defaultTimeoutSeconds = 5;
    /**
     * 读超时时间(秒)
     */
    private int readTimeoutSeconds = 30;
    /**
     * 读空闲时间(秒)
     */
    private int readerIdleTimeSeconds = 25;
    /**
     * 默认连接密码
     */
    private String defaultPassword = "ClueCon";
    /**
     * 是否禁用公共执行器
     */
    private boolean disablePublicExecutor = false;
    /**
     * 是否开启性能监控 - 事件驱动-业务逻辑处理时间
     */
    private boolean performance = false;
    /**
     * 性能监控耗时阈值(毫秒)
     */
    private long performanceCostTime = 200L;
    /**
     * 是否开启事件性能监控 - fs 产生事件与应用接收到事件时间差
     */
    private boolean eventPerformance = false;
    /**
     * 事件性能监控耗时阈值(毫秒)
     */
    private long eventPerformanceCostTime = 200L;
    /**
     * 订阅的事件名列表
     */
    private List<String> events = new ArrayList<>();
    /**
     * ESL 服务节点列表
     */
    private List<ServerProperties> servers = new ArrayList<>();

    /**
     * ESL 服务节点配置。
     */
    @Data
    public static class ServerProperties {
        /**
         * 主机地址
         */
        private String host;
        /**
         * 端口
         */
        private int port;
        /**
         * 连接超时时间(秒)
         */
        private int timeoutSeconds;
        /**
         * 连接密码, 为空时使用 defaultPassword
         */
        private String password;
    }
}
