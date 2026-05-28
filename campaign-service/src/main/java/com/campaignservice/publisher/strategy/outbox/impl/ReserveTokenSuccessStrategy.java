package com.campaignservice.publisher.strategy.outbox.impl;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.model.OutboxEvent;
import com.campaignservice.publisher.strategy.outbox.OutboxEventStrategy;
import com.campaignservice.service.CampaignMessagingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchpad.common.event.TokensReservedSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReserveTokenSuccessStrategy implements OutboxEventStrategy {

    private final ObjectMapper objectMapper;
    private final CampaignMessagingService messagingService;

    @Override
    public OutboxEvent.EventType getOutboxEventStrategy() {
        return OutboxEvent.EventType.RESERVE_TOKENS_SUCCESS;
    }

    @Override
    public void publish(OutboxEvent outboxEvent, CorrelationData correlationData) throws JsonProcessingException {
        TokensReservedSuccessEvent event = objectMapper.readValue(outboxEvent.getPayload(), TokensReservedSuccessEvent.class);
        messagingService.convertAndSend(
                RabbitMQConfig.EXCHANGE_SAGA,
                RabbitMQConfig.ROUTING_SUCCESS,
                event,
                correlationData
        );
    }
}
