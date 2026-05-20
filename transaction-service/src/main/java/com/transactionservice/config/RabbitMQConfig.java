package com.transactionservice.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String SAGA_EXCHANGE = "saga.direct.exchange";
    public static final String SAGA_REPLY_QUEUE = "transaction.saga.reply.queue";

    public static final String ROUTING_RESERVE = "tokens.reserve";
    public static final String ROUTING_SUCCESS = "tokens.reserved.success";
    public static final String ROUTING_FAILED = "tokens.reserved.failed";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(SAGA_EXCHANGE);
    }

    @Bean
    public Queue replyQueue() {
        return new Queue(SAGA_REPLY_QUEUE, true);
    }

    @Bean
    public Binding successBinding(Queue replyQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(replyQueue).to(directExchange).with(ROUTING_SUCCESS);
    }

    @Bean
    public Binding failedBinding(Queue replyQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(replyQueue).to(directExchange).with(ROUTING_FAILED);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setConfirmCallback(
                (correlationData,
                 ack,
                 cause) -> {
                    if (ack) {
                        log.info(
                                "RabbitMQ ACK correlationId={}",
                                correlationData != null
                                        ? correlationData.getId()
                                        : ""
                        );
                    } else {
                        log.error(
                                "RabbitMQ NACK correlationId={} cause={}",
                                correlationData != null
                                        ? correlationData.getId()
                                        : "",
                                cause
                        );
                    }
                }
        );
        template.setReturnsCallback(returned -> {
            log.error(
                    """
                    Message returned:
                    exchange={}
                    routingKey={}
                    replyCode={}
                    replyText={}
                    """,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()
            );
        });
        return template;
    }
}
