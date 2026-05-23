package com.transactionservice.publisher.strategy.outbox.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchpad.common.event.ReserveTokensEvent;
import com.transactionservice.config.RabbitMQConfig;
import com.transactionservice.model.outbox.OutboxEvent;
import com.transactionservice.model.outbox.OutboxType;
import com.transactionservice.publisher.strategy.outbox.OutboxEventStrategy;
import com.transactionservice.service.TransactionMessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReserveTokenStrategy implements OutboxEventStrategy {

    private final ObjectMapper objectMapper;
    private final TransactionMessagingService transactionMessagingService;

    @Override
    public OutboxType getOutboxEventStrategy() {
        return OutboxType.RESERVE_TOKENS;
    }

    @Override
    public void publish(OutboxEvent outboxEvent, CorrelationData correlationData) throws JsonProcessingException {
        ReserveTokensEvent reserveTokensEvent = objectMapper.readValue(outboxEvent.getPayload(), ReserveTokensEvent.class);
        transactionMessagingService.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ROUTING_RESERVE,
                reserveTokensEvent,
                correlationData
        );
    }
}
