package com.streama.config;

import com.streama.entity.constants.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class AuditRabbitConfig {

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(Constants.RABBIT_AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange requestDlxExchange() {
        return new DirectExchange(Constants.RABBIT_AUDIT_REQUEST_DLX, true, false);
    }

    @Bean
    public DirectExchange resultDlxExchange() {
        return new DirectExchange(Constants.RABBIT_AUDIT_RESULT_DLX, true, false);
    }

    @Bean
    public Queue auditRequestQueue() {
        return QueueBuilder.durable(Constants.RABBIT_AUDIT_REQUEST_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.RABBIT_AUDIT_REQUEST_DLX)
                .withArgument("x-dead-letter-routing-key", Constants.RABBIT_AUDIT_REQUEST_QUEUE)
                .build();
    }

    @Bean
    public Queue auditRequestDlq() {
        return QueueBuilder.durable(Constants.RABBIT_AUDIT_REQUEST_DLQ).build();
    }

    @Bean
    public Queue auditResultQueue() {
        return QueueBuilder.durable(Constants.RABBIT_AUDIT_RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", Constants.RABBIT_AUDIT_RESULT_DLX)
                .withArgument("x-dead-letter-routing-key", Constants.RABBIT_AUDIT_RESULT_QUEUE)
                .build();
    }

    @Bean
    public Queue auditResultDlq() {
        return QueueBuilder.durable(Constants.RABBIT_AUDIT_RESULT_DLQ).build();
    }

    @Bean
    public Binding auditRequestBinding(Queue auditRequestQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditRequestQueue).to(auditExchange).with(Constants.RABBIT_AUDIT_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding auditResultBinding(Queue auditResultQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditResultQueue).to(auditExchange).with(Constants.RABBIT_AUDIT_RESULT_ROUTING_KEY);
    }

    @Bean
    public Binding auditRequestDlqBinding(Queue auditRequestDlq, DirectExchange requestDlxExchange) {
        return BindingBuilder.bind(auditRequestDlq).to(requestDlxExchange).with(Constants.RABBIT_AUDIT_REQUEST_QUEUE);
    }

    @Bean
    public Binding auditResultDlqBinding(Queue auditResultDlq, DirectExchange resultDlxExchange) {
        return BindingBuilder.bind(auditResultDlq).to(resultDlxExchange).with(Constants.RABBIT_AUDIT_RESULT_QUEUE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
