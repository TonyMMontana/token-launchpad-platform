package com.transactionservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequestDto(
        UUID userId,
        Long campaignId,
        BigDecimal amount
) {
}
