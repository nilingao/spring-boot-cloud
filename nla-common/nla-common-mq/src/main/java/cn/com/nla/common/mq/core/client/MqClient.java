package cn.com.nla.common.mq.core.client;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** 动态拓扑声明与发布；保持旧方法名，声明错误向调用方抛出。 @author TZY */
public class MqClient {
    private final AmqpAdmin admin;
    private final RabbitTemplate template;

    public MqClient(AmqpAdmin admin, RabbitTemplate template) {
        this.admin = Objects.requireNonNull(admin);
        this.template = Objects.requireNonNull(template);
    }

    /** 兼容手动构造；自动配置使用消费方的独立 RabbitTemplate。 */
    public MqClient(RabbitAdmin admin) { this(admin, admin.getRabbitTemplate()); }

    public Binding binding(String exchangeName, String routingKey, String queueName, String type, boolean delayed) {
        return binding(exchangeName, routingKey, queueName, type, delayed, Map.of(), Map.of());
    }

    /** 兼容单 Map：headers 类型作为绑定参数，其余类型作为队列参数（例如 DLX/TTL）。 */
    public Binding binding(String exchangeName, String routingKey, String queueName, String type, boolean delayed,
            Map<String, Object> arguments) {
        return ExchangeTypes.HEADERS.equals(type)
            ? binding(exchangeName, routingKey, queueName, type, delayed, Map.of(), arguments)
            : binding(exchangeName, routingKey, queueName, type, delayed, arguments, Map.of());
    }

    public Binding binding(String exchangeName, String routingKey, String queueName, String type, boolean delayed,
            Map<String, Object> queueArguments, Map<String, Object> bindingArguments) {
        Assert.hasText(exchangeName, "exchangeName must not be empty");
        Assert.hasText(queueName, "queueName must not be empty");
        Assert.hasText(type, "exchange type must not be empty");
        Objects.requireNonNull(routingKey, "routingKey");
        ExchangeBuilder builder = new ExchangeBuilder(exchangeName, type).durable(true);
        if (delayed) { builder.delayed(); }
        Exchange exchange = builder.build();
        Queue queue = new Queue(queueName, true, false, false, copy(queueArguments));
        Binding binding = new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName, routingKey, copy(bindingArguments));
        admin.declareExchange(exchange);
        String declared = admin.declareQueue(queue);
        if (declared == null) { throw new IllegalStateException("Queue declaration returned no result: " + queueName); }
        admin.declareBinding(binding);
        return binding;
    }

    public void removeBinding(String exchangeName, String routingKey, String queueName) {
        removeBinding(new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName, routingKey, Map.of()));
    }
    public void removeBinding(Binding binding) { admin.removeBinding(Objects.requireNonNull(binding)); }
    public void deleteExchange(String exchange) { admin.deleteExchange(exchange); }
    public void deleteQueue(String queue) { admin.deleteQueue(queue); }

    public void send(String exchangeName, String routingKey, Object message) {
        template.convertAndSend(exchangeName, routingKey, message);
    }
    public void send(String exchangeName, String routingKey, Object message, CorrelationData correlation) {
        template.convertAndSend(exchangeName, routingKey, message, correlation);
    }

    /** 第 2 参数是 routing key，延迟单位是毫秒；需 RabbitMQ delayed-message 插件。 */
    public void sendDelay(String exchangeName, String routingKey, Object message, Integer delayMillis) {
        Objects.requireNonNull(delayMillis, "delayMillis");
        sendDelay(exchangeName, routingKey, message, delayMillis.longValue());
    }
    public void sendDelay(String exchangeName, String routingKey, Object message, long delayMillis) {
        Assert.isTrue(delayMillis >= 0 && delayMillis <= MessageProperties.X_DELAY_MAX, "delayMillis is outside RabbitMQ x-delay range");
        template.convertAndSend(exchangeName, routingKey, message, outbound -> {
            outbound.getMessageProperties().setDelayLong(delayMillis);
            return outbound;
        });
    }
    private static Map<String, Object> copy(Map<String, Object> arguments) {
        return arguments == null ? new HashMap<>() : new HashMap<>(arguments);
    }
}
