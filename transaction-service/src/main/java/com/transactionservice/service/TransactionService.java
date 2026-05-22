package com.transactionservice.service;

import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.CreateTransactionResponseDto;
import com.transactionservice.event.messaging.TokensReservedFailedEvent;
import com.transactionservice.event.messaging.TokensReservedSuccessEvent;

import java.util.UUID;

public interface TransactionService {
    CreateTransactionResponseDto createTransaction(UUID userId, CreateTransactionRequestDto requestDto);

    void handleSuccessSagaReply(TokensReservedSuccessEvent event);

    void handleFailedSagaReply(TokensReservedFailedEvent event);
}
