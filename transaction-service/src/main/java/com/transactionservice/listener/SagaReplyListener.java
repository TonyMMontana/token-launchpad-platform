package com.transactionservice.listener;

import com.transactionservice.config.RabbitMQConfig;
import com.transactionservice.event.TokensReservedFailedEvent;
import com.transactionservice.event.TokensReservedSuccessEvent;
import com.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.SAGA_REPLY_QUEUE)
public class SagaReplyListener {
    private final TransactionService transactionService;

    @RabbitHandler
    public void handleSuccessReply(TokensReservedSuccessEvent event) {
        transactionService.handleSuccessSagaReply(event);
    }
    @RabbitHandler
    public void handleFailedReply(TokensReservedFailedEvent event) {
        transactionService.handleFailedSagaReply(event);
    }
}
