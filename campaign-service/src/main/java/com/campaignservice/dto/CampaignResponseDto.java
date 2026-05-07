package com.campaignservice.dto;

import com.campaignservice.model.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignResponseDto(
        Long id,
        String tokenName,
        BigDecimal targetAmount,
        LocalDateTime targetDate,
        Status status
) {
}
