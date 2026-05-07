package com.campaignservice.service;

import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.dto.CreateCampaignRequestDto;
import com.campaignservice.dto.CreateCampaignResponseDto;


public interface CampaignService {
    CreateCampaignResponseDto createCampaign(CreateCampaignRequestDto requestDto);

    void launchCampaign(Long campaignId);

    CampaignResponseDto getCampaign(Long id);
}
