package com.campaignservice.service;

public interface RabbitMQService {
    void sendMessage(Long campaignId, Long duration);
}
