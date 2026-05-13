package com.transactionservice.event;

import java.math.BigDecimal;

public record TokensReservedFailedEvent(
        Long transactionId,
        Long campaignId,
        BigDecimal amount,
        String reason
) {
}
