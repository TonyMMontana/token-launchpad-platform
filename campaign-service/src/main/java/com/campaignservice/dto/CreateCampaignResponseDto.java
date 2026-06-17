package com.campaignservice.dto;

import com.campaignservice.model.Campaign;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCampaignResponseDto(
        Long id,
        String tokenName,
        BigDecimal targetAmount,
        Instant targetDate,
        Campaign.CampaignStatus campaignStatus
) {
}
