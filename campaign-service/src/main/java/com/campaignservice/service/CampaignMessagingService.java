package com.campaignservice.service;

public interface CampaignMessagingService {
    void sendCreateCampaignMessage(Long campaignId, Long duration);

    void sendSagaReply(String routingKey, Object event);
}
