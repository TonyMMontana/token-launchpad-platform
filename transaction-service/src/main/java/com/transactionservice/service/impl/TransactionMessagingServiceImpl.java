package com.transactionservice.service.impl;

import com.transactionservice.service.TransactionMessagingService;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionMessagingServiceImpl implements TransactionMessagingService {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void convertAndSend(String exchange, String routingKey, Object event, CorrelationData correlationData) {
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                event,
                correlationData
        );
    }
}
