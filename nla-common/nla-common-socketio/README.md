# Socket.IO 与 TCP 技术封装

阶段 3.4 的 `cn.com.nla:nla-common-socketio`，使用 netty-socketio 2.0.14，Netty 由 Boot BOM 统一管理。默认关闭，通过 Boot 4 `.imports` 自动配置接入。

```xml
<dependency>
    <groupId>cn.com.nla</groupId>
    <artifactId>nla-common-socketio</artifactId>
</dependency>
```

```yaml
socket-io:
  enabled: true
  host: 0.0.0.0
  port: 9092
  context: /socket.io
  store: memory
```

Socket.IO 使用独立端口。反向代理需把 `/socket.io/` 路由至此端口，并转发 WebSocket Upgrade；`context` 配置值不带末尾 `/`，客户端请求路径带 `/`。支持 Engine.IO 4 / Socket.IO 4 的 WebSocket 与 Polling。多节点 Polling 仍需代理层会话粘滞，Redis 不会迁移活跃的 TCP/HTTP 连接。

## 监听器接入

旧 `pool.NamespaceListener`、`pool.EventListener<T>` 迁至 `basic.listener`，方法名不变。namespace 可以单独注册为 Spring Bean；事件引用的 namespace 即便不是独立 Bean，也会注册一次。同名 namespace 必须引用同一个监听对象；同 namespace 的重复事件名启动失败。

```java
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import cn.com.nla.common.socketio.basic.listener.EventListener;
import com.corundumstudio.socketio.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatSocketConfiguration {
    @Bean
    NamespaceListener chatNamespace() {
        return new NamespaceListener() {
            private SocketIOServer server;
            public String getNamespaceName() { return "/chat"; }
            public SocketIOServer getSocketIOServer() { return server; }
            public void setSocketIOServer(SocketIOServer server) { this.server = server; }
            public void onConnect(SocketIOClient client) { client.joinRoom("public"); }
            public void onDisconnect(SocketIOClient client) { }
        };
    }

    @Bean
    EventListener<String> echoEvent(NamespaceListener chatNamespace) {
        return new EventListener<>() {
            public Class<String> getEventClass() { return String.class; }
            public String getEventName() { return "echo"; }
            public NamespaceListener getNamespace() { return chatNamespace; }
            public void onData(SocketIOClient client, String data, AckRequest ack) {
                ack.sendAckData(data);
            }
        };
    }
}
```

客户端：`io("http://localhost:9092/chat", {path: "/socket.io"})`，再 `socket.emit("echo", "你好", response => console.log(response))`。

Spring Bean 上的 `@OnConnect`、`@OnDisconnect`、`@OnEvent` 同样支持，默认注册到根 namespace。注册器在单例初始化后挂载监听，再由 `SmartLifecycle` 启动服务器，避免旧 `@PostConstruct` 过早启动和自动配置循环注入。

## 鉴权和定制

提供一个原生 `AuthorizationListener` Bean 即可接管握手鉴权；当前未设置业务鉴权时沿用库的允许连接行为。生产业务接入时应提供自己的令牌校验，技术模块不读取旧 JWT/OAuth2 的静态上下文。用户/坐席房间、二维码过期校验、Sa-Token 与租户隔离属于业务迁移阶段。

可通过 `SocketIoConfigurationCustomizer` Bean 按 Spring 顺序设置原生 `Configuration`（TLS、transport、JSON support、异常监听等），也可替换 `SocketIOServer` 或 `StoreFactory` Bean。自动创建的生命周期会管理所注入的服务器；如需自行管理，可同时替换 `SocketIoLifecycle`。

## Redisson 集群模式

Redisson 是可选依赖，单节点模式无需 Redis。集群消费方应引入已有 `nla-common-redis`，或自行提供 RedissonClient；该 client 由消费方负责关闭。

```yaml
socket-io:
  enabled: true
  store: redisson
  redis-prefix: nla:socketio:chat
```

同集群节点使用相同前缀，不同业务集群使用不同前缀。Map、会话和 Topic 均受此前缀隔离。启用 redisson 而缺少 client 时启动失败。

`SocketIoRedissonStoreFactory` 使用实时 RMap，修复旧 RedisTemplate Hash 快照不能写回的问题。断开连接删除对应会话；工厂关闭仅移除自己注册的 7 类 Topic 监听，不关闭共享 client，不调用 `removeAllListeners()`。异常退出留下的会话数据尚无自动 TTL 清理。

默认 `SocketIoRedisCodec` 使用独立 Jackson 2，支持中文、应用 POJO、UUID、LocalDateTime 和 byte[]；不依赖基线的默认 Redis codec。类型允许范围为 Socket.IO 内部包、`cn.com.nla` 和常用 Java 包。其他根包的 DTO 可通过自行声明工厂并传入自定义 `Codec` 支持。网络事件的 JSON support 可由 customizer 定制（例如注册 JavaTimeModule），Redis codec 与网络 JSON support 是各自独立的序列化边界。

旧 socketio `Message` 保留 code/message/data 和二维码业务码 0/1/2，位于 `basic.domain.Message`；`OutType` 位于 `basic.enums`，业务接入时需与 HTTP `R` 的状态码分别核对。

## 配置默认值

| 属性 | 默认值 |
|---|---|
| enabled / auto-start | false / true |
| name / host / port | socket-service / 0.0.0.0 / 9092 |
| context / origin | /socket.io / 库默认行为 |
| boss-count / work-count | 1 / 0（库按 CPU 自动选择） |
| max-frame-payload-length / max-http-content-length | 1048576 / 1048576 字节 |
| allow-custom-requests | false |
| upgrade-timeout / ping-timeout / ping-interval | 10000 / 30000 / 12000 毫秒 |
| store / redis-prefix | memory / nla:socketio |

`name` 保留配置兼容，不做 Nacos 注册；`work-count` 从旧 100 调整为库默认值，`allow-custom-requests` 从旧 true 调整为 false。`auto-start=false` 创建服务器和监听器但不监听端口，适合手动启动和测试。

## TCP 基座迁移

旧 starter-netty 的 15 个有效 Java 文件迁入 `basic.netty` 与 `core.netty`。4 个只有注释的 MainServer/MainServerHandler/VirtualServerClient/Biz100000039 文件保留在旧目录，未迁入。TCP 与 Socket.IO 是两种独立协议，TCP 基座显式启动，不自动开放额外端口。

保留 msgCode + 时间扩展钩子 + serial + length + 可选 CRC16 + body 的线格式，以及 `Msg100000039` 模型。`MessageDecoder` 默认整帧上限 1 MiB，可用构造器调整；支持半包、粘包、自定义消息工厂和时间头，未知码/负长度/超限/CRC 错误会拒绝解码。自定义 `Message` 需提供无参构造器。

`NettyServer` 和 `NettyClient` 实现 AutoCloseable，支持重复关闭、绑定失败清理；客户端重连间隔 1 秒，连接超时 5 秒。`DefaultBizFactory` 改为显式传入 Spring BeanFactory，保留 `biz%09d` Bean 命名规则。线程池由业务调用方提供和管理，模块不自动创建业务执行器。

## 已验证范围

```powershell
$env:JAVA_HOME = 'D:/develop/Jdk/jdk-21.0.12.1'
& 'D:/develop/Maven/apache-maven-3.9.9/bin/mvn.cmd' -o -B `
  -pl nla-common/nla-common-socketio -am `
  '-Dtest=SocketIoContractTest,SocketIoProtocolTest,NettyContractTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

21 项测试：12 个配置/Redis 契约、3 个真实本机 Socket.IO 协议测试、6 个 TCP 契约测试（包括真实客户端/服务端往返）。Windows / JDK21 / Boot4 / Netty4.2 下全部通过。Redis 部分使用模拟客户端和真实 codec 字节往返；真实 Redis 多节点、代理层 Polling 粘滞、生产负载与业务鉴权联调尚未验证。
