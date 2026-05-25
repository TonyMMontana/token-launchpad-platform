package com.campaignservice.service.impl;

import com.campaignservice.service.CampaignMessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignMessagingServiceImpl implements CampaignMessagingService {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void convertAndSendWithDelay(String exchange, String routing, Object message, Long delayInMs) {
        rabbitTemplate.convertAndSend(
                exchange,
                routing,
                message,
                messagePostProcessor -> {
                    messagePostProcessor.getMessageProperties().setDelayLong(delayInMs);
                    return messagePostProcessor;
                }
        );
    }

    @Override
    public void convertAndSend(String exchange, String routing, Object event, CorrelationData correlationData) {
        rabbitTemplate.convertAndSend(
                exchange,
                routing,
                event,
                correlationData
        );
    }
}
