package com.transactionservice.event.messaging;

import java.math.BigDecimal;

public record TokensReservedFailedEvent(
        Long transactionId,
        Long campaignId,
        BigDecimal amount,
        String reason
) {
}
