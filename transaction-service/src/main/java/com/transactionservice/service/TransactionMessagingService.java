package com.transactionservice.service;

import com.transactionservice.event.ReserveTokensEvent;

public interface TransactionMessagingService {
    void convertAndSend(String routingKey, Object event);
}
