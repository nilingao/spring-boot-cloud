package cn.com.nla.common.mq.core.config;

import cn.com.nla.common.mq.core.client.MqClient;
import cn.com.nla.common.mq.core.converter.MqMessageConverter;
import cn.com.nla.common.mq.core.properties.MqProperties;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 在 Boot 注册模板/消费者之前提供 converter，连接生命周期由 Boot 管理。 @author TZY */
@AutoConfiguration(beforeName = "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "nla.mq", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MqProperties.class)
public class MqAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jsonMessageConverter(MqProperties properties) {
        return new MqMessageConverter(properties.getTrustedPackages());
    }

    @Bean
    @ConditionalOnMissingBean(AmqpAdmin.class)
    public RabbitAdmin rabbitAdmin(ConnectionFactory factory, MqProperties properties) {
        RabbitAdmin admin = new RabbitAdmin(factory);
        admin.setIgnoreDeclarationExceptions(false);
        admin.setAutoStartup(properties.isAutoDeclare());
        return admin;
    }

    @Bean
    @ConditionalOnMissingBean(MqClient.class)
    public MqClient mqClient(AmqpAdmin admin, RabbitTemplate template) { return new MqClient(admin, template); }
}
