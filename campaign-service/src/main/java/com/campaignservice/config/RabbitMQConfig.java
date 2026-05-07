package com.campaignservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "campaign.launch.queue";
    public static final String EXCHANGE_NAME = "campaign.delayed.exchange";
    public static final String ROUTING_KEY = "campaign.start";

    @Bean
    public Queue launchQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_NAME, "x-delayed-message", true, false, args);
    }

    @Bean
    public Binding binding(Queue launchQueue, CustomExchange delayedExchange) {
        return BindingBuilder.bind(launchQueue).to(delayedExchange).with(ROUTING_KEY).noargs();
    }
}