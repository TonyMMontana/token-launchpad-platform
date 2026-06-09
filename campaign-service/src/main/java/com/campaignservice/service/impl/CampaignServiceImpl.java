package com.campaignservice.service.impl;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.dto.CreateCampaignRequestDto;
import com.campaignservice.dto.CreateCampaignResponseDto;
import com.campaignservice.model.Campaign;
import com.campaignservice.model.CampaignReservation;
import com.campaignservice.model.OutboxEvent;
import com.campaignservice.repository.CampaignRepository;
import com.campaignservice.repository.OutboxEventRepository;
import com.campaignservice.repository.ReservationRepository;
import com.campaignservice.service.CampaignMessagingService;
import com.campaignservice.service.CampaignService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.launchpad.common.event.ReserveTokensEvent;
import com.launchpad.common.event.TokensReservedFailedEvent;
import com.launchpad.common.event.TokensReservedSuccessEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final ObjectMapper objectMapper;
    private final CampaignRepository campaignRepository;
    private final OutboxEventRepository outboxRepository;
    private final ReservationRepository reservationRepository;
    private final CampaignMessagingService campaignMessagingService;

    @Override
    public CreateCampaignResponseDto createCampaign(CreateCampaignRequestDto requestDto) {
        long delay = Duration.between(LocalDateTime.now(), requestDto.startTime()).toMillis();
        if (delay < 0) {
            throw new IllegalArgumentException("Campaign start time must be in the future!");
        }
        Campaign campaign = mapToModel(requestDto);

        Campaign saved = campaignRepository.save(campaign);
        campaignMessagingService.convertAndSendWithDelay(
                RabbitMQConfig.CAMPAIGN_DELAYED_EXCHANGE,
                RabbitMQConfig.ROUTING_CAMPAIGN_START,
                saved.getId(),
                delay
        );

        return new CreateCampaignResponseDto(saved.getId(), saved.getTokenName(), saved.getTargetAmount(), saved.getStartTime(), saved.getCampaignStatus());
    }

    @Override
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void launchCampaign(Long campaignId) {
        campaignRepository.findById(campaignId).ifPresent(campaign -> {
            campaign.setCampaignStatus(Campaign.CampaignStatus.LIVE);
            campaignRepository.save(campaign);
        });
    }

    @Override
    @Cacheable(value = "campaigns", key = "#campaignId")
    public CampaignResponseDto getCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow(()
                -> new EntityNotFoundException("There is no campaign with id: " + campaignId));

        return new CampaignResponseDto(campaign.getId(), campaign.getTokenName(),
                campaign.getTargetAmount(), campaign.getStartTime(), campaign.getCampaignStatus());
    }

    @Override
    @Transactional
    @CacheEvict(value = "campaigns", key = "#reserveTokensEvent.campaignId()")
    public void reserveTokens(ReserveTokensEvent reserveTokensEvent) {
        LocalDateTime now = LocalDateTime.now();
        int claimed = reservationRepository.claimProcessingReservation(
                reserveTokensEvent.transactionId(),
                reserveTokensEvent.campaignId(),
                reserveTokensEvent.amount(),
                now,
                now
        );
        if (claimed == 0) {
            Optional<CampaignReservation> existing = reservationRepository.findByTransactionId(reserveTokensEvent.transactionId());
            existing.ifPresent(this::resendReply);
            return;
        }

        try {
            int rowsUpdated = campaignRepository.reserveTokensAtomically(
                    reserveTokensEvent.campaignId(),
                    reserveTokensEvent.amount()
            );

            if (rowsUpdated == 1) {
                CampaignReservation reservation = updateReservationStatus(reserveTokensEvent.transactionId(), CampaignReservation.ReservationStatus.RESERVED, null);

                OutboxEvent outboxEvent = createOutboxEvent(reservation, OutboxEvent.EventType.RESERVE_TOKENS_SUCCESS, null);
                outboxRepository.save(outboxEvent);
            } else {
                throw new IllegalStateException("Campaign sold out, inactive, or capacity exceeded.");
            }
        } catch (Exception e) {
            CampaignReservation reservation = updateReservationStatus(reserveTokensEvent.transactionId(), CampaignReservation.ReservationStatus.FAILED, e.getMessage());

            OutboxEvent outboxEvent = createOutboxEvent(reservation, OutboxEvent.EventType.RESERVE_TOKENS_FAILURE, e.getMessage());
            outboxRepository.save(outboxEvent);
        }
    }

    @Override
    public Page<CampaignResponseDto> getCampaigns(Pageable pageable) {
        Page<Campaign> campaignPage = campaignRepository.findAll(pageable);
        return campaignPage.map(campaign ->
                new CampaignResponseDto(
                        campaign.getId(),
                        campaign.getTokenName(),
                        campaign.getTargetAmount(),
                        campaign.getStartTime(),
                        campaign.getCampaignStatus()
                )
        );
    }

    private Campaign mapToModel(CreateCampaignRequestDto requestDto) {
        Campaign campaign = new Campaign();
        campaign.setStartTime(requestDto.startTime());
        campaign.setTokenName(requestDto.tokenName());
        campaign.setTargetAmount(requestDto.targetAmount());
        campaign.setCampaignStatus(Campaign.CampaignStatus.PENDING);
        return campaign;
    }

    private void resendReply(CampaignReservation reservation) {
        if (reservation.getStatus() == CampaignReservation.ReservationStatus.PROCESSING) {
            log.info("Reservation {} is already being processed", reservation.getId());
            return;
        }

        OutboxEvent outboxEvent = outboxRepository.findByAggregateId(reservation.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Critical Error: Reservation exists but no Outbox record found for reservation id: " + reservation.getId())
                );
        if (outboxEvent.getStatus() == OutboxEvent.OutboxStatus.PROCESSING) {
            log.info("Outbox event is already being processed for reservation id {}", reservation.getId());
            return;
        }
        outboxEvent.setStatus(OutboxEvent.OutboxStatus.NEW);
        outboxRepository.save(outboxEvent);
    }

    private OutboxEvent createOutboxEvent(CampaignReservation reservation, OutboxEvent.EventType eventType, String errorMessage) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateId(reservation.getId());
        outboxEvent.setEventType(eventType);
        outboxEvent.setStatus(OutboxEvent.OutboxStatus.NEW);
        outboxEvent.setRetryCount(0);
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEvent.setNextAttemptAt(LocalDateTime.now());

        Object payload = getPayload(reservation, eventType, errorMessage);
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        try {
            outboxEvent.setPayload(objectWriter.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Unable to write payload for outboxEvent for reservation {}", reservation.getId(), e);
            throw new RuntimeException(e);
        }
        return outboxEvent;
    }

    private static Object getPayload(CampaignReservation reservation, OutboxEvent.EventType eventType, String errorMessage) {
        return switch (eventType) {
            case RESERVE_TOKENS_SUCCESS -> new TokensReservedSuccessEvent(
                    reservation.getTransactionId(),
                    reservation.getCampaignId(),
                    reservation.getAmount()
            );
            case RESERVE_TOKENS_FAILURE -> new TokensReservedFailedEvent(
                    reservation.getTransactionId(),
                    reservation.getCampaignId(),
                    reservation.getAmount(),
                    errorMessage
            );
        };
    }

    private CampaignReservation updateReservationStatus(Long transactionId, CampaignReservation.ReservationStatus reservationStatus, String failureReason) {
        CampaignReservation reservation = reservationRepository.findByTransactionId(transactionId).orElseThrow(
                () -> new EntityNotFoundException("There is no reservation with id: " + transactionId)
        );
        reservation.setStatus(reservationStatus);
        reservation.setFailureReason(failureReason);
        reservation.setUpdatedAt(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

}
