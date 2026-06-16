package com.campaignservice.service;

import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.dto.CreateCampaignRequestDto;
import com.campaignservice.dto.CreateCampaignResponseDto;
import com.launchpad.common.event.ReserveTokensEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CampaignService {
    CreateCampaignResponseDto createCampaign(CreateCampaignRequestDto requestDto);

    void launchCampaign(Long campaignId);

    CampaignResponseDto getCampaign(Long id);

    void reserveTokens(ReserveTokensEvent reserveTokensEvent);

    Page<CampaignResponseDto> getCampaigns(Pageable pageable);
}
