package cn.com.nla.common.mq;

import cn.com.nla.common.mq.basic.constant.MqConstant;
import cn.com.nla.common.mq.core.client.MqClient;
import cn.com.nla.common.mq.core.config.MqAutoConfiguration;
import cn.com.nla.common.mq.core.converter.MqMessageConverter;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.core.ParameterizedTypeReference;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqContractTest {
    private final ApplicationContextRunner contexts = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MqAutoConfiguration.class));
    private ApplicationContextRunner boot() {
        return contexts.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
            .withPropertyValues("nla.mq.enabled=true", "nla.mq.auto-declare=false",
                "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.listener.direct.auto-startup=false");
    }
    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cn.com.nla.common.mq")
    static class WideScan { }

    @Test void disabledWideScanCreatesNoClientConverterOrConnections() {
        contexts.withUserConfiguration(WideScan.class).run(context -> assertThat(context).hasNotFailed()
            .doesNotHaveBean(MqClient.class).doesNotHaveBean(MessageConverter.class)
            .doesNotHaveBean(ConnectionFactory.class).doesNotHaveBean(AmqpAdmin.class));
    }
    @Test void enabledWithoutConsumerInfrastructureFailsClearly() {
        contexts.withPropertyValues("nla.mq.enabled=true").run(context -> assertThat(context).hasFailed());
    }
    @Test void bootTemplateUsesSameConverterAndPreservesConnectionAndPublishSettings() {
        boot().withPropertyValues("spring.rabbitmq.host=127.0.0.1", "spring.rabbitmq.port=5673",
            "spring.rabbitmq.virtual-host=/integration", "spring.rabbitmq.publisher-confirm-type=correlated",
            "spring.rabbitmq.publisher-returns=true", "spring.rabbitmq.template.mandatory=true")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MqClient.class).hasSingleBean(AmqpAdmin.class)
                    .hasSingleBean(MessageConverter.class).hasSingleBean(RabbitTemplate.class);
                MessageConverter converter = context.getBean(MessageConverter.class);
                RabbitTemplate template = context.getBean(RabbitTemplate.class);
                assertSame(converter, template.getMessageConverter());
                assertSame(template, ReflectionTestUtils.getField(context.getBean(MqClient.class), "template"));
                var factory = context.getBean(CachingConnectionFactory.class);
                assertEquals(5673, factory.getPort()); assertEquals("/integration", factory.getVirtualHost());
                assertTrue(factory.isPublisherConfirms()); assertTrue(factory.isPublisherReturns());
                assertTrue(template.isMandatoryFor(new Message(new byte[0])));
                assertFalse(context.getBean(RabbitAdmin.class).isAutoStartup());
            });
    }
    @Test void typedListenerInheritsConverterManualAckAndPrefetchFromBoot() {
        boot().withBean(Listener.class, Listener::new)
            .withPropertyValues("spring.rabbitmq.listener.simple.acknowledge-mode=manual", "spring.rabbitmq.listener.simple.prefetch=7")
            .run(context -> {
                assertThat(context).hasNotFailed();
                var container = (AbstractMessageListenerContainer) context.getBean(RabbitListenerEndpointRegistry.class)
                    .getListenerContainer("mq-contract-listener");
                assertNotNull(container); assertFalse(container.isRunning());
                assertSame(context.getBean(MessageConverter.class),
                    ReflectionTestUtils.getField(container.getMessageListener(), "messageConverter"));
                assertEquals(AcknowledgeMode.MANUAL, container.getAcknowledgeMode());
                assertEquals(7, ReflectionTestUtils.getField(container, "prefetchCount"));
                var properties = new MessageProperties(); properties.setContentType("application/json");
                var incoming = new Message("{\"text\":\"监听消息\",\"time\":\"2026-10-10T12:00:00\"}".getBytes(StandardCharsets.UTF_8), properties);
                ((MessagingMessageListenerAdapter) container.getMessageListener())
                    .onMessage(incoming, mock(com.rabbitmq.client.Channel.class));
                assertEquals(new Payload("监听消息", LocalDateTime.of(2026, 10, 10, 12, 0)),
                    context.getBean(Listener.class).received);
            });
    }
    @Test void directListenerAlsoInheritsConverterAndManualAck() {
        boot().withBean(Listener.class, Listener::new).withPropertyValues("spring.rabbitmq.listener.type=direct",
            "spring.rabbitmq.listener.direct.acknowledge-mode=manual")
            .run(context -> {
                assertThat(context).hasNotFailed();
                var container = (AbstractMessageListenerContainer) context.getBean(RabbitListenerEndpointRegistry.class)
                    .getListenerContainer("mq-contract-listener");
                assertSame(context.getBean(MessageConverter.class),
                    ReflectionTestUtils.getField(container.getMessageListener(), "messageConverter"));
                assertEquals(AcknowledgeMode.MANUAL, container.getAcknowledgeMode()); assertFalse(container.isRunning());
            });
    }
    @Test void consumerCanOverrideConverterAdminTemplateAndClient() {
        MessageConverter converter = mock(MessageConverter.class);
        AmqpAdmin admin = mock(AmqpAdmin.class);
        ConnectionFactory factory = mock(ConnectionFactory.class);
        RabbitTemplate template = new RabbitTemplate(factory);
        MqClient client = new MqClient(admin, template);
        boot().withBean(MessageConverter.class, () -> converter).withBean(AmqpAdmin.class, () -> admin)
            .withBean(RabbitTemplate.class, () -> template).withBean(MqClient.class, () -> client)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(MessageConverter.class).hasSingleBean(AmqpAdmin.class)
                    .hasSingleBean(MqClient.class).hasSingleBean(RabbitTemplate.class);
                assertSame(client, context.getBean(MqClient.class));
            });
        verifyNoInteractions(admin, factory);
    }
    @Test void invalidTrustedPackageConfigurationFailsBinding() {
        boot().withPropertyValues("nla.mq.trusted-packages[0]=").run(context -> assertThat(context).hasFailed());
    }
    @Test void jsonPojoChineseTimeAndGenericListRoundTrip() {
        var converter = new MqMessageConverter("cn.com.nla.common.mq");
        Payload payload = new Payload("中文消息", LocalDateTime.of(2026, 10, 10, 12, 0));
        Message message = converter.toMessage(payload, new MessageProperties());
        assertThat(message.getMessageProperties().getContentType()).contains("json");
        assertEquals(payload, converter.fromMessage(message));
        Message list = converter.toMessage(List.of(payload), new MessageProperties());
        list.getMessageProperties().setInferredArgumentType(new ParameterizedTypeReference<List<Payload>>() { }.getType());
        assertEquals(List.of(payload), converter.fromMessage(list));
    }
    @Test void inferredConcreteListenerTypeDoesNotRequireSenderJavaTypeHeader() {
        var converter = new MqMessageConverter("cn.com.nla");
        var properties = new MessageProperties(); properties.setContentType("application/json");
        properties.setInferredArgumentType(Payload.class);
        Message message = new Message("{\"text\":\"外部消息\",\"time\":\"2026-10-10T12:00:00\"}".getBytes(StandardCharsets.UTF_8), properties);
        assertEquals(new Payload("外部消息", LocalDateTime.of(2026, 10, 10, 12, 0)), converter.fromMessage(message));
    }
    @Test void senderTypeHeaderRequiresExactTrustedDtoPackage() {
        var converter = new MqMessageConverter("cn.com.nla");
        Message message = converter.toMessage(new Payload("消息", LocalDateTime.of(2026, 10, 10, 12, 0)), new MessageProperties());
        assertThrows(IllegalArgumentException.class, () -> converter.fromMessage(message));
        assertInstanceOf(Payload.class, new MqMessageConverter("cn.com.nla.common.mq").fromMessage(message));
    }
    @Test void vendorJsonAndCharsetAreRecognizedAndPlainTextAndBytesPreserved() {
        var converter = new MqMessageConverter("cn.com.nla");
        var json = new MessageProperties(); json.setContentType("application/vnd.nla+json;charset=UTF-8");
        assertEquals(Map.of("text", "你好"), converter.fromMessage(new Message("{\"text\":\"你好\"}".getBytes(StandardCharsets.UTF_8), json)));
        var text = new MessageProperties(); text.setContentType("text/plain;charset=UTF-8");
        assertEquals("旧文本", converter.fromMessage(new Message("旧文本".getBytes(StandardCharsets.UTF_8), text)));
        byte[] bytes = {1, 2, 3}; Message binary = converter.toMessage(bytes, new MessageProperties());
        assertArrayEquals(bytes, (byte[]) converter.fromMessage(binary));
        var serialized = new MessageProperties(); serialized.setContentType(MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT);
        assertArrayEquals(bytes, (byte[]) converter.fromMessage(new Message(bytes, serialized)));
    }
    @Test void legacyBrokerNamesRemainUnchanged() {
        assertEquals("qr_exchange", MqConstant.QR_EXCHANGE); assertEquals("qr_queue", MqConstant.QR_QUEUE);
        assertEquals("qr_routing_key", MqConstant.QR_ROUTING_KEY);
        assertEquals("dead_letter_exchange", MqConstant.DEAD_LETTER_EXCHANGE);
        assertEquals("dead_letter_queue", MqConstant.DEAD_LETTER_QUEUE);
        assertEquals("dead_letter_routing_key", MqConstant.DEAD_LETTER_ROUTING_KEY);
    }
    public record Payload(String text, LocalDateTime time) { }
    public static class Listener {
        private Payload received;
        @RabbitListener(id = "mq-contract-listener", queues = "contract.queue") public void receive(Payload payload) {
            received = payload;
        }
    }
}
