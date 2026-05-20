package com.transactionservice.service;

import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.CreateTransactionResponseDto;
import com.transactionservice.event.messaging.TokensReservedFailedEvent;
import com.transactionservice.event.messaging.TokensReservedSuccessEvent;

public interface TransactionService {
    CreateTransactionResponseDto createTransaction(CreateTransactionRequestDto requestDto);

    void handleSuccessSagaReply(TokensReservedSuccessEvent event);

    void handleFailedSagaReply(TokensReservedFailedEvent event);
}
