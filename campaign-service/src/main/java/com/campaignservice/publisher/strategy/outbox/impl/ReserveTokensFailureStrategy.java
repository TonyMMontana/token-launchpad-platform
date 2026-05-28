package com.campaignservice.publisher.strategy.outbox.impl;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.model.OutboxEvent;
import com.campaignservice.publisher.strategy.outbox.OutboxEventStrategy;
import com.campaignservice.service.CampaignMessagingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchpad.common.event.TokensReservedFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReserveTokensFailureStrategy implements OutboxEventStrategy {
    private final ObjectMapper objectMapper;
    private final CampaignMessagingService messagingService;

    @Override
    public OutboxEvent.EventType getOutboxEventStrategy() {
        return OutboxEvent.EventType.RESERVE_TOKENS_FAILURE;
    }

    @Override
    public void publish(OutboxEvent outboxEvent, CorrelationData correlationData) throws JsonProcessingException {
        TokensReservedFailedEvent event = objectMapper.readValue(outboxEvent.getPayload(), TokensReservedFailedEvent.class);
        messagingService.convertAndSend(
                RabbitMQConfig.EXCHANGE_SAGA,
                RabbitMQConfig.ROUTING_FAILED,
                event,
                correlationData
        );
    }
}
