package com.campaignservice.service.impl;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.service.CampaignMessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignMessagingServiceImpl implements CampaignMessagingService {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendCreateCampaignMessage(Long campaignId, Long delayInMs) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CAMPAIGN_DELAYED_EXCHANGE,
                RabbitMQConfig.ROUTING_CAMPAIGN_START,
                campaignId,
                messagePostProcessor -> {
                    messagePostProcessor.getMessageProperties().setDelayLong(delayInMs);
                    return messagePostProcessor;
                }
        );
    }

    @Override
    public void sendSagaReply(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SAGA, routingKey, event);
    }
}
