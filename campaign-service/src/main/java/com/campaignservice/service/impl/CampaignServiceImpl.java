package com.campaignservice.service.impl;

import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.dto.CreateCampaignRequestDto;
import com.campaignservice.dto.CreateCampaignResponseDto;
import com.campaignservice.model.Campaign;
import com.campaignservice.model.Status;
import com.campaignservice.repository.CampaignRepository;
import com.campaignservice.service.CampaignService;
import com.campaignservice.service.RabbitMQService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final RabbitMQService rabbitMQService;

    @Override
    public CreateCampaignResponseDto createCampaign(CreateCampaignRequestDto requestDto) {
        long delay = Duration.between(LocalDateTime.now(), requestDto.startTime()).toMillis();
        if (delay < 0) {
            throw new IllegalArgumentException("Campaign start time must be in the future!");
        }
        Campaign campaign = mapToModel(requestDto);

        Campaign saved = campaignRepository.save(campaign);
        rabbitMQService.sendMessage(saved.getId(), delay);

        return new CreateCampaignResponseDto(saved.getId(), saved.getTokenName(), saved.getTargetAmount(), saved.getStartTime(), saved.getStatus());
    }

    @Override
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void launchCampaign(Long campaignId) {
        campaignRepository.findById(campaignId).ifPresent(campaign -> {
            campaign.setStatus(Status.LIVE);
            campaignRepository.save(campaign);
        });
    }

    @Override
    @Cacheable(value = "campaigns", key = "#campaignId")
    public CampaignResponseDto getCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow(()
                -> new EntityNotFoundException("There is no campaign with id: " + campaignId));

        return new CampaignResponseDto(campaign.getId(), campaign.getTokenName(),
                campaign.getTargetAmount(), campaign.getStartTime(), campaign.getStatus());
    }

    private Campaign mapToModel(CreateCampaignRequestDto requestDto) {
        Campaign campaign = new Campaign();
        campaign.setStartTime(requestDto.startTime());
        campaign.setTokenName(requestDto.tokenName());
        campaign.setTargetAmount(requestDto.targetAmount());
        campaign.setStatus(Status.PENDING);
        return campaign;
    }
}
