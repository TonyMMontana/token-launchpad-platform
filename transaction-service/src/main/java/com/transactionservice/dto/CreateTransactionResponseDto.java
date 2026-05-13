package com.transactionservice.dto;

import com.transactionservice.model.Status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateTransactionResponseDto(
        UUID userId,
        Long campaignId,
        BigDecimal amount,
        Status status,
        List<String> exceptions
) {
}
