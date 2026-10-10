# nla-common-video

GB28181 SIP、ZLMediaKit 与 ONVIF 技术封装，运行基线为 JDK 21 / Spring Boot 4。
包分为 `cn.com.nla.common.video.basic`（协议模型、枚举）和 `core`（协议实现、缓存与订阅）。

模块已登记到 `nla-common` 和 `nla-common-bom`；消费方添加依赖即可加载自动配置：

```xml
<dependency>
    <groupId>cn.com.nla</groupId>
    <artifactId>nla-common-video</artifactId>
</dependency>
```

默认关闭。启用前须准备 Redis、实现业务 SPI，并配置 SIP 参数：

```yaml
video:
  enabled: true
  sip:
    id: "34020000002000000001"
    domain: "3402000000"
    port: 5062
    bind-ip: 0.0.0.0
    advertised-ip: 192.0.2.10 # 替换为设备可达的本机/公网地址
    password: "replace-with-device-password"
  settings:
    server-id: "video-node-01" # 每个节点使用独立 ID
    play-timeout: 18 # 秒
    sip-log: false
```

旧前缀 `sip.*` 和 `video-settings.*` 分别改为 `video.sip.*`、`video.settings.*`。
`bind-ip` 控制监听地址，`advertised-ip` 控制信令中对外公布的地址；节点注册记录 SIP 端口。
与 FreeSWITCH 同进程运行时为两者配置不同的监听端口，video 使用独立 SIP 栈名和按类型注入。

消费方在阶段 6.5 实现 `core.service.video` 下的业务 SPI：设备、通道、国标流、代理流、推流、
平台目录、上级平台、流媒体服务器、移动位置与告警服务。启用配置时必须提供
`DeviceChannelVoService` Bean，其他服务会在启动 Runner 或对应协议流程中调用。
流媒体服务器配置通过 `MediaServerVoService` 提供。消费方提供 HTTP Controller 接收 ZLM Hook，
再调用 `MediaHookServer`；本模块不注册 HTTP 路由。

`CurrentUserProvider` 可由消费方实现并接入登录上下文，默认返回 `null`，下载流程沿用默认用户。
保留的流鉴权 SPI 由业务层接入现有认证体系。

协议结果 `ProtocolResult` 保留 **code=0 表示成功**；Controller 返回项目 `R<T>` 时须转换 code 语义。
`VideoRestResult<T>` 是 `CompletableFuture<T>`，消费方可按 Web 框架的异步机制返回或适配。
Redis 协议 Topic 统一传输 `SerializationUtils.serialize(...)` 得到的 `byte[]`，接收方反序列化一次。
Hook 事件按类型共享订阅、按字段过滤；删除和续期按同一 HookKey 身份操作。

本地验证命令：

```text
mvn -o -B -pl nla-common/nla-common-video -am -Dtest=VideoContractTest -Dsurefire.failIfNoSpecifiedTests=false test
```

测试覆盖配置门控、同名 Bean 注入、回调隔离/超时、订阅释放、Redis 序列化、模拟 ZLM HTTP 和
真实本机 SIP UDP 收包及传输端口释放。真实设备注册、心跳、点播/回放、ZLM RTP 与 ONVIF 联调仍需外部环境。
