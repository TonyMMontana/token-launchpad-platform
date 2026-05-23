package com.transactionservice.service;

import com.launchpad.common.event.TokensReservedFailedEvent;
import com.launchpad.common.event.TokensReservedSuccessEvent;
import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.CreateTransactionResponseDto;

import java.util.UUID;

public interface TransactionService {
    CreateTransactionResponseDto createTransaction(UUID userId, CreateTransactionRequestDto requestDto);

    void handleSuccessSagaReply(TokensReservedSuccessEvent event);

    void handleFailedSagaReply(TokensReservedFailedEvent event);
}
