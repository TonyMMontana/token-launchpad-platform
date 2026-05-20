package com.transactionservice.event.messaging;

import java.math.BigDecimal;

public record ReserveTokensEvent(
        Long transactionId,
        Long campaignId,
        BigDecimal amount
) {
}
