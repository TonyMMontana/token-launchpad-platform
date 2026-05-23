package com.launchpad.common.event;

import java.math.BigDecimal;

public record ReserveTokensEvent(
        Long transactionId,
        Long campaignId,
        BigDecimal amount
) {
}
