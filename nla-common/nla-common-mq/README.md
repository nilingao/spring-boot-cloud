# nla-common-mq

RabbitMQ 技术封装，适配 JDK21 / Spring Boot4 / Spring AMQP4 / Jackson3。保留旧封装的拓扑、发送方法及二维码/死信资源名称，不包含二维码登录、Redis、JWT、Socket.IO 或业务消费者。

## 接入

消费方通过 `nla-common-bom` 管理模块版本，并引入 Boot AMQP starter：

```xml
<dependency>
    <groupId>cn.com.nla</groupId>
    <artifactId>nla-common-mq</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

本模块编译依赖只包含 `spring-rabbit`、Boot 自动配置基础库和 Jackson3，不传递引入 Boot AMQP starter。也可以不用 starter，由消费方自行提供 `ConnectionFactory` 和 `RabbitTemplate`。启用封装但缺少这些基础设施时，应用启动失败。

```yaml
nla:
  mq:
    enabled: true                  # 默认 false，仅开启本模块封装
    auto-declare: true             # 默认 true，RabbitAdmin 自动声明上下文中的拓扑 Bean
    trusted-packages:
      - cn.com.nla.sms.dto         # 替换为消费方实际 DTO 所在的完整包名
spring:
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: /
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual  # 对齐旧业务显式 basicAck 的消费者
```

`nla.mq.*` 管封装开关、自动声明和可信包；连接、发布重试、confirm/return、监听器并发/prefetch/ACK 等仍使用 Boot 的 `spring.rabbitmq.*`。使用 direct 监听器时改用 `spring.rabbitmq.listener.type=direct` 和 `listener.direct.acknowledge-mode=manual`。

`enabled=false` 时本模块不注册客户端、转换器或 RabbitAdmin；消费方宽范围组件扫描也不能绕过门控。该开关不会关闭消费方另外引入的 Boot AMQP starter 或其消费者。只依赖本模块且保持默认关闭时，不会创建连接或启动消费者。

自动配置在 Boot Rabbit 自动配置之前注册转换器和 RabbitAdmin，`MqClient` 使用消费方的 `RabbitTemplate`，保留其 converter、confirm/return、重试及 customizer 设置。消费方可分别提供 `MessageConverter`、`AmqpAdmin`、`RabbitTemplate`、`MqClient` Bean；本模块按类型退让。若提供自己的 template，转换器及其他 template 设置也由消费方负责。

## 拓扑声明

```java
import cn.com.nla.common.mq.basic.constant.MqConstant;
import cn.com.nla.common.mq.core.client.MqClient;
import org.springframework.amqp.core.ExchangeTypes;
import java.util.Map;

// 在业务初始化流程中显式调用，模块本身不会声明二维码拓扑。
mqClient.binding(MqConstant.DEAD_LETTER_EXCHANGE, MqConstant.DEAD_LETTER_ROUTING_KEY,
    MqConstant.DEAD_LETTER_QUEUE, ExchangeTypes.DIRECT, false);
mqClient.binding(MqConstant.QR_EXCHANGE, MqConstant.QR_ROUTING_KEY,
    MqConstant.QR_QUEUE, ExchangeTypes.DIRECT, false,
    Map.of("x-dead-letter-exchange", MqConstant.DEAD_LETTER_EXCHANGE,
        "x-dead-letter-routing-key", MqConstant.DEAD_LETTER_ROUTING_KEY),
    Map.of());
```

完整方法的最后两个参数分别是 `queueArguments` 和 `bindingArguments`：DLX/TTL 放队列参数，headers exchange 的 `x-match` 及匹配字段放绑定参数。输入 Map 会复制，可以传 `Map.of(...)`。

保留旧单 Map 重载：`type=headers` 时解释为绑定参数，其他类型解释为队列参数。旧封装把同一 Map 同时写入两处，现在不会重复注入；确实需要两处参数时使用完整重载。默认 exchange/queue 持久化，queue 非 exclusive、非 auto-delete。

声明顺序为 exchange → queue → binding，声明异常向调用方抛出，不再忽略。已存在资源的类型/参数不兼容时会失败；前面已经声明的资源不会自动回滚。`auto-declare=false` 只关闭本模块 RabbitAdmin 的自动声明，不禁止显式 `binding(...)` 调用，也不替代监听容器自己的声明配置。`removeBinding`、`deleteExchange`、`deleteQueue` 仅在显式调用时执行。移除带参数的 headers binding 时传入原 `Binding` 对象。

保持 6 个旧资源名称：`qr_exchange` / `qr_routing_key` / `qr_queue`、`dead_letter_exchange` / `dead_letter_routing_key` / `dead_letter_queue`。

## 消息与监听

普通出站对象（包括 String）使用 Jackson3 JSON，`byte[]` 保持原始字节。入站支持 `application/json`、vendor `+json` 及带 charset 的 JSON；`text/*` 按 charset、content-encoding 或默认 UTF-8 解码；其他内容返回 `byte[]`。

`@RabbitListener` 的具体 DTO 或 `List<DTO>` 参数可通过 inferred argument type 转换，不要求发送方提供 Java 类型头。没有具体监听类型、需依赖 `__TypeId__` 等发送方类型头时，配置 DTO 所在的**完整包名**。Spring AMQP4 的可信包只做精确匹配，`cn.com.nla` 不包含其子包；默认仅信任 `cn.com.nla`（框架另默认信任 `java.lang` / `java.util`）。单独的 `*` 表示信任所有包，只有明确接受全部发送方类型时才配置；包通配模式不受支持。

旧转换器可能对普通对象使用 Java Serialization；新模块不自动反序列化 `application/x-java-serialized-object`，返回原始字节。迁移业务消费者时同步使用 JSON 或消费方自定义 converter，不能假定旧队列中的序列化消息能直接作为 DTO 接收。泛型 conversion hint 会传递给 JSON converter，中文与 `LocalDateTime` 往返已验证。

本模块不全局修改 ACK 模式。旧业务消费者保留显式 `basicAck` 时，消费方必须配置 MANUAL，失败重入队/死信策略由业务定义。

## 发布与延迟

```java
mqClient.send("exchange", "routing.key", payload);
var correlation = new org.springframework.amqp.rabbit.connection.CorrelationData("request-1");
mqClient.send("exchange", "routing.key", payload, correlation);
mqClient.sendDelay("delayed.exchange", "routing.key", payload, 5_000L);
```

`sendDelay` 第二参数是 routing key，延迟单位为毫秒，范围 `0..MessageProperties.X_DELAY_MAX`；保留 Integer 重载并新增 long 重载，使用 AMQP4 的 `setDelayLong`。目标 exchange 必须预先以 `binding(..., delayed=true, ...)` 或外部部署方式声明为 delayed exchange，RabbitMQ 必须安装并启用匹配版本的 `rabbitmq_delayed_message_exchange` 插件。`x-delay` 不等同于队列 TTL/DLX 延时机制。

`send` 返回只代表同步发送调用结束，不能作为 broker 接收或消费者处理成功的证明。需要投递确认时配置 correlated confirms，处理 `CorrelationData` future/confirm callback；不可路由消息配置 publisher returns、mandatory，并注册 `RabbitTemplate.ReturnsCallback`（或发送时带有 ID 的 CorrelationData）。这些策略由消费方配置，本模块不安装默认业务回调。同步转换/连接/声明错误会向调用方传播。

手动兼容构造 `new MqClient(RabbitAdmin)` 仍可用，但使用 admin 的内部 template；需要继承 Boot 发布配置时使用注入的 `MqClient` 或 `new MqClient(amqpAdmin, rabbitTemplate)`。

## 验证边界

`MqContractTest` / `MqClientTest` 覆盖默认关闭与宽扫描、启用缺依赖失败、Boot 发布和 simple/direct 监听配置、用户 Bean 覆盖、真实监听适配器的 DTO 转换、可信包、中文/时间/泛型 JSON、文本/字节、拓扑参数分离、声明/发送失败，以及真实 RabbitTemplate 经模拟 AMQP Channel 的 persistent JSON 和长整型 x-delay 发布。

测试不连接真实 RabbitMQ。真实 broker 拓扑/重连、delayed-message 插件投递、confirm/return 回调与业务 ACK/DLX 流程仍需环境联调；二维码消费者与登录业务留后续业务迁移阶段。
