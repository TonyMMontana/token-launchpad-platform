package com.campaignservice.dto;

import com.campaignservice.model.Campaign;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCampaignResponseDto(
        Long id,
        String tokenName,
        BigDecimal targetAmount,
        LocalDateTime targetDate,
        Campaign.CampaignStatus campaignStatus
) {
}
