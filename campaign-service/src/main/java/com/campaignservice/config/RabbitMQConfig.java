package com.campaignservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String CAMPAIGN_LAUNCH_QUEUE = "campaign.launch.queue";
    public static final String CAMPAIGN_DELAYED_EXCHANGE = "campaign.delayed.exchange";
    public static final String ROUTING_CAMPAIGN_START = "campaign.start";

    public static final String EXCHANGE_SAGA = "saga.direct.exchange";
    public static final String QUEUE_RESERVE = "campaign.saga.reserve.queue";

    public static final String ROUTING_RESERVE = "tokens.reserve";
    public static final String ROUTING_SUCCESS = "tokens.reserved.success";
    public static final String ROUTING_FAILED = "tokens.reserved.failed";
    @Bean
    public Queue launchQueue() {
        return new Queue(CAMPAIGN_LAUNCH_QUEUE, true);
    }

    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(CAMPAIGN_DELAYED_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    public Binding binding(Queue launchQueue, CustomExchange delayedExchange) {
        return BindingBuilder.bind(launchQueue).to(delayedExchange).with(ROUTING_CAMPAIGN_START).noargs();
    }

    @Bean
    public DirectExchange sagaExchange() {
        return new DirectExchange(EXCHANGE_SAGA);
    }

    @Bean
    public Queue reserveQueue() {
        return new Queue(QUEUE_RESERVE, true);
    }

    @Bean
    public Binding reserveBinding(Queue reserveQueue, DirectExchange sagaExchange) {
        return BindingBuilder.bind(reserveQueue).to(sagaExchange).with(ROUTING_RESERVE);
    }
}