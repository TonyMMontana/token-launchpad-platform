package com.launchpad.common.event;

import java.math.BigDecimal;

public record TokensReservedSuccessEvent(
        Long transactionId,
        Long campaignId,
        BigDecimal amount
) {
}
