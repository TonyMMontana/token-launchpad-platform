package com.campaignservice.service.impl;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.dto.CreateCampaignRequestDto;
import com.campaignservice.dto.CreateCampaignResponseDto;
import com.campaignservice.event.ReserveTokensEvent;
import com.campaignservice.event.TokensReservedFailedEvent;
import com.campaignservice.event.TokensReservedSuccessEvent;
import com.campaignservice.model.Campaign;
import com.campaignservice.model.Status;
import com.campaignservice.repository.CampaignRepository;
import com.campaignservice.service.CampaignMessagingService;
import com.campaignservice.service.CampaignService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final CampaignMessagingService campaignMessagingService;

    @Override
    public CreateCampaignResponseDto createCampaign(CreateCampaignRequestDto requestDto) {
        long delay = Duration.between(LocalDateTime.now(), requestDto.startTime()).toMillis();
        if (delay < 0) {
            throw new IllegalArgumentException("Campaign start time must be in the future!");
        }
        Campaign campaign = mapToModel(requestDto);

        Campaign saved = campaignRepository.save(campaign);
        campaignMessagingService.sendCreateCampaignMessage(saved.getId(), delay);

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

    @Override
    @Transactional
    @CacheEvict(value = "campaigns", key = "#reserveTokensEvent.campaignId()")
    public void reserveTokens(ReserveTokensEvent reserveTokensEvent) {
        try {
            int rowsUpdated = campaignRepository.reserveTokensAtomically(
                    reserveTokensEvent.campaignId(),
                    reserveTokensEvent.amount()
            );

            if (rowsUpdated == 1) {
                campaignMessagingService.sendSagaReply(
                        RabbitMQConfig.ROUTING_SUCCESS,
                        new TokensReservedSuccessEvent(
                                reserveTokensEvent.transactionId(),
                                reserveTokensEvent.campaignId(),
                                reserveTokensEvent.amount()
                        )
                );
            } else {
                throw new IllegalStateException("Campaign sold out, inactive, or capacity exceeded.");
            }
        } catch (Exception e) {
            campaignMessagingService.sendSagaReply(
                    RabbitMQConfig.ROUTING_FAILED,
                    new TokensReservedFailedEvent(
                            reserveTokensEvent.transactionId(),
                            reserveTokensEvent.campaignId(),
                            reserveTokensEvent.amount(),
                            e.getMessage()
                    )
            );
        }
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
