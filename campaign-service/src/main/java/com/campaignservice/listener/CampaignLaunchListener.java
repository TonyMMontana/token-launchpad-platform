package com.campaignservice.listener;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CampaignLaunchListener {
    private final CampaignService campaignService;

    @RabbitListener(queues = RabbitMQConfig.CAMPAIGN_LAUNCH_QUEUE)
    public void receive(Long campaignId) {
        campaignService.launchCampaign(campaignId);
    }
}
