package com.transactionservice.dto;

import com.transactionservice.model.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateTransactionResponseDto(
        Long transactionId,
        UUID userId,
        Long campaignId,
        BigDecimal amount,
        TransactionStatus transactionStatus,
        List<String> exceptions
) {
}
