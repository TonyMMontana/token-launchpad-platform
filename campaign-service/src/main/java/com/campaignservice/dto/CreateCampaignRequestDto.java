package com.campaignservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCampaignRequestDto(
        String tokenName,
        Instant startTime,
        BigDecimal targetAmount
) {
}
