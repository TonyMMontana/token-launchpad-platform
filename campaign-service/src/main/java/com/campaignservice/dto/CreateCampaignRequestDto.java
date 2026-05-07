package com.campaignservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCampaignRequestDto(
        String tokenName,
        LocalDateTime startTime,
        BigDecimal targetAmount
) {
}
