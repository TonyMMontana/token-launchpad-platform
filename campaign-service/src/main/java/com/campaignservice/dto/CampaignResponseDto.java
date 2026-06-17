package com.campaignservice.dto;

import com.campaignservice.model.Campaign;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public record CampaignResponseDto(
        Long id,
        String tokenName,
        BigDecimal targetAmount,
        BigDecimal tokensSold,
        Instant targetDate,
        Campaign.CampaignStatus campaignStatus
) {
}
