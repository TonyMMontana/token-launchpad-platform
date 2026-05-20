package com.transactionservice.service;

import org.springframework.amqp.rabbit.connection.CorrelationData;

public interface TransactionMessagingService {
    void convertAndSend(String exchange, String routingKey, Object event, CorrelationData correlationData);
}
