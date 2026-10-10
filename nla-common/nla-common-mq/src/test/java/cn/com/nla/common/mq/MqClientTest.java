package cn.com.nla.common.mq;

import cn.com.nla.common.mq.basic.constant.MqConstant;
import cn.com.nla.common.mq.core.client.MqClient;
import cn.com.nla.common.mq.core.converter.MqMessageConverter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MqClientTest {
    private final AmqpAdmin admin = mock(AmqpAdmin.class);
    private final RabbitTemplate template = mock(RabbitTemplate.class);
    private final MqClient client = new MqClient(admin, template);

    @Test void qrDeadLetterArgumentsBelongToQueueAndInputMapRemainsUnchanged() {
        when(admin.declareQueue(any(Queue.class))).thenAnswer(call -> ((Queue) call.getArgument(0)).getName());
        var args = Map.<String, Object>of("x-dead-letter-exchange", MqConstant.DEAD_LETTER_EXCHANGE,
            "x-dead-letter-routing-key", MqConstant.DEAD_LETTER_ROUTING_KEY);
        Binding binding = client.binding(MqConstant.QR_EXCHANGE, MqConstant.QR_ROUTING_KEY, MqConstant.QR_QUEUE,
            ExchangeTypes.DIRECT, false, args);
        ArgumentCaptor<Queue> queue = ArgumentCaptor.forClass(Queue.class);
        var order = inOrder(admin); order.verify(admin).declareExchange(any(Exchange.class));
        order.verify(admin).declareQueue(queue.capture()); order.verify(admin).declareBinding(binding);
        assertEquals(args, queue.getValue().getArguments()); assertTrue(binding.getArguments().isEmpty());
        assertTrue(queue.getValue().isDurable()); assertFalse(queue.getValue().isExclusive());
        assertEquals(2, args.size());
    }
    @Test void headersArgumentsBelongToBindingAndCanBeCombinedWithIndependentQueueArguments() {
        when(admin.declareQueue(any(Queue.class))).thenReturn("queue");
        var headers = Map.<String, Object>of("x-match", "all", "kind", "notice");
        Binding legacy = client.binding("exchange", "", "queue", ExchangeTypes.HEADERS, false, headers);
        assertEquals(headers, legacy.getArguments());
        Binding full = client.binding("exchange", "", "queue", ExchangeTypes.HEADERS, false,
            Map.of("x-message-ttl", 1000), headers);
        assertEquals(headers, full.getArguments());
        ArgumentCaptor<Queue> queues = ArgumentCaptor.forClass(Queue.class);
        verify(admin, times(2)).declareQueue(queues.capture());
        assertTrue(queues.getAllValues().get(0).getArguments().isEmpty());
        assertEquals(1000, queues.getAllValues().get(1).getArguments().get("x-message-ttl"));
    }
    @Test void delayedExchangeFlagIsAppliedAndFanoutAllowsEmptyRoutingKey() {
        when(admin.declareQueue(any(Queue.class))).thenReturn("queue");
        client.binding("delayed", "", "queue", ExchangeTypes.FANOUT, true);
        ArgumentCaptor<Exchange> exchange = ArgumentCaptor.forClass(Exchange.class);
        verify(admin).declareExchange(exchange.capture()); assertTrue(exchange.getValue().isDelayed());
        assertTrue(exchange.getValue().isDurable());
    }
    @Test void declarationFailureStopsRemainingStepsAndIsNotSwallowed() {
        doThrow(new AmqpException("precondition failed")).when(admin).declareExchange(any());
        assertThrows(AmqpException.class, () -> client.binding("exchange", "route", "queue", ExchangeTypes.DIRECT, false));
        verify(admin, never()).declareQueue(any(Queue.class)); verify(admin, never()).declareBinding(any());
        reset(admin); when(admin.declareQueue(any(Queue.class))).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> client.binding("exchange", "route", "queue", ExchangeTypes.DIRECT, false));
        verify(admin, never()).declareBinding(any());
    }
    @Test void sendingAndCorrelationUseSuppliedTemplateAndFailuresPropagate() {
        client.send("exchange", "route", "payload"); verify(template).convertAndSend("exchange", "route", "payload");
        CorrelationData correlation = new CorrelationData("request-1");
        client.send("exchange", "route", "payload", correlation);
        verify(template).convertAndSend("exchange", "route", "payload", correlation);
        doThrow(new AmqpException("unavailable")).when(template).convertAndSend("exchange", "route", "fail");
        assertThrows(AmqpException.class, () -> client.send("exchange", "route", "fail"));
    }
    @Test void invalidDelayAndTopologyAreRejectedBeforeSideEffects() {
        assertThrows(IllegalArgumentException.class, () -> client.sendDelay("exchange", "route", "data", -1L));
        assertThrows(IllegalArgumentException.class, () -> client.sendDelay("exchange", "route", "data", MessageProperties.X_DELAY_MAX + 1));
        assertThrows(NullPointerException.class, () -> client.sendDelay("exchange", "route", "data", (Integer) null));
        assertThrows(IllegalArgumentException.class, () -> client.binding("", "route", "queue", ExchangeTypes.DIRECT, false));
        verifyNoInteractions(admin, template);
    }
    @Test void removeAndDeleteDelegateExplicitlyWithoutStartupCleanup() {
        client.removeBinding("exchange", "route", "queue");
        ArgumentCaptor<Binding> binding = ArgumentCaptor.forClass(Binding.class);
        verify(admin).removeBinding(binding.capture()); assertEquals("route", binding.getValue().getRoutingKey());
        client.deleteExchange("exchange"); client.deleteQueue("queue");
        verify(admin).deleteExchange("exchange"); verify(admin).deleteQueue("queue");
    }
    @Test void realRabbitTemplateConvertsPersistentJsonAndAddsDelayBeforeChannelPublish() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class); Connection connection = mock(Connection.class);
        Channel channel = mock(Channel.class);
        when(factory.createConnection()).thenReturn(connection);
        when(connection.createChannel(false)).thenReturn(channel); when(channel.isOpen()).thenReturn(true);
        RabbitTemplate actual = new RabbitTemplate(factory); actual.setMessageConverter(new MqMessageConverter("cn.com.nla"));
        actual.setMandatory(true);
        actual.setReturnsCallback(mock(RabbitTemplate.ReturnsCallback.class));
        var sender = new MqClient(admin, actual);
        sender.sendDelay("delay.exchange", "delay.route", Map.of("text", "中文"), 3_000_000_000L);
        ArgumentCaptor<AMQP.BasicProperties> properties = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(channel).basicPublish(eq("delay.exchange"), eq("delay.route"), eq(true), properties.capture(), body.capture());
        assertThat(properties.getValue().getContentType()).contains("json");
        assertEquals(2, properties.getValue().getDeliveryMode());
        assertEquals(3_000_000_000L, ((Number) properties.getValue().getHeaders().get("x-delay")).longValue());
        assertThat(new String(body.getValue(), StandardCharsets.UTF_8)).contains("中文");
        verify(connection).close();
    }
}
