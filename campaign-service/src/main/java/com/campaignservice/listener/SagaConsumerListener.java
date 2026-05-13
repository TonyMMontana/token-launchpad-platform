package com.campaignservice.listener;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.event.ReserveTokensEvent;
import com.campaignservice.service.CampaignService;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@RabbitListener(queues = RabbitMQConfig.QUEUE_RESERVE)
public class SagaConsumerListener {
    private final CampaignService campaignService;

    @RabbitHandler
    public void receiveEvent(ReserveTokensEvent reserveTokensEvent) {
        campaignService.reserveTokens(reserveTokensEvent);
    }
}
