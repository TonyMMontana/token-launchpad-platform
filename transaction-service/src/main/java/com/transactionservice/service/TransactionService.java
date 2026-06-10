package com.transactionservice.service;

import com.launchpad.common.event.TokensReservedFailedEvent;
import com.launchpad.common.event.TokensReservedSuccessEvent;
import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface TransactionService {
    TransactionResponseDto createTransaction(UUID userId, UUID idempotencyKey, CreateTransactionRequestDto requestDto);

    void handleSuccessSagaReply(TokensReservedSuccessEvent event);

    void handleFailedSagaReply(TokensReservedFailedEvent event);

    TransactionResponseDto getTransaction(Long transactionId, UUID userId, Set<String> roles);

    Page<TransactionResponseDto> getTransactions(UUID userId, Pageable pageable);
}
