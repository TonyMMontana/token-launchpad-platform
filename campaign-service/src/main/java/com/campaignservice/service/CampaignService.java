package com.campaignservice.service;

import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.dto.CreateCampaignRequestDto;
import com.campaignservice.dto.CreateCampaignResponseDto;
import com.campaignservice.event.ReserveTokensEvent;


public interface CampaignService {
    CreateCampaignResponseDto createCampaign(CreateCampaignRequestDto requestDto);

    void launchCampaign(Long campaignId);

    CampaignResponseDto getCampaign(Long id);

    void reserveTokens(ReserveTokensEvent reserveTokensEvent);
}
