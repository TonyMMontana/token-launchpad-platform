package com.campaignservice.service.impl;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.service.RabbitMQService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitMQServiceImpl implements RabbitMQService {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendMessage(Long campaignId, Long delayInMs) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                campaignId,
                messagePostProcessor -> {
                    messagePostProcessor.getMessageProperties().setDelayLong(delayInMs);
                    return messagePostProcessor;
                }
        );
    }
}
