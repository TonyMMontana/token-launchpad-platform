package com.campaignservice.publisher.strategy.outbox;

import com.campaignservice.model.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.amqp.rabbit.connection.CorrelationData;

public interface OutboxEventStrategy {

    OutboxEvent.EventType getOutboxEventStrategy();
    void publish(OutboxEvent outboxEvent , CorrelationData correlationData) throws JsonProcessingException;
}
