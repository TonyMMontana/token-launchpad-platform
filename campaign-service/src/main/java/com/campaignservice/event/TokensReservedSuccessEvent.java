package com.campaignservice.event;

import java.math.BigDecimal;

public record TokensReservedSuccessEvent(
        Long transactionId,
        Long campaignId,
        BigDecimal amount
) {
}
