package com.transactionservice.publisher.strategy.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.transactionservice.model.outbox.OutboxEvent;
import com.transactionservice.model.outbox.OutboxType;
import org.springframework.amqp.rabbit.connection.CorrelationData;

public interface OutboxEventStrategy {

    OutboxType getOutboxEventStrategy();
    void publish(OutboxEvent outboxEvent , CorrelationData correlationData) throws JsonProcessingException;
}
