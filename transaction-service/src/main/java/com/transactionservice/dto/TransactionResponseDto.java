package com.transactionservice.dto;

import com.transactionservice.model.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionResponseDto(
        Long transactionId,
        UUID userId,
        Long campaignId,
        BigDecimal amount,
        TransactionStatus transactionStatus,
        LocalDateTime createdAt,
        List<String> exceptions
) {
}
