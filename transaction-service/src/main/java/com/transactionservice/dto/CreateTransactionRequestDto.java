package com.transactionservice.dto;

import java.math.BigDecimal;

public record CreateTransactionRequestDto(
        Long campaignId,
        BigDecimal amount
) {
}
