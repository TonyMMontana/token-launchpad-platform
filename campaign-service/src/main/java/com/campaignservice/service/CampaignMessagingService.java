package com.campaignservice.service;

import org.springframework.amqp.rabbit.connection.CorrelationData;

public interface CampaignMessagingService {
    void convertAndSendWithDelay(String exchange, String routing, Object message, Long delayInMs);

    void convertAndSend(String exchange, String routingKey, Object event, CorrelationData correlationData);
}
