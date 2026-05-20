package com.transactionservice.event.messaging;

import java.math.BigDecimal;

public record TokensReservedSuccessEvent(
        Long transactionId,
        Long campaignId,
        BigDecimal amount
) {
}
