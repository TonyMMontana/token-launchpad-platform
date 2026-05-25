package com.campaignservice.service.impl;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.dto.CreateCampaignRequestDto;
import com.campaignservice.dto.CreateCampaignResponseDto;
import com.campaignservice.model.Campaign;
import com.campaignservice.model.CampaignReservation;
import com.campaignservice.repository.CampaignRepository;
import com.campaignservice.repository.ReservationRepository;
import com.campaignservice.service.CampaignMessagingService;
import com.campaignservice.service.CampaignService;
import com.launchpad.common.event.ReserveTokensEvent;
import com.launchpad.common.event.TokensReservedFailedEvent;
import com.launchpad.common.event.TokensReservedSuccessEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
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
        campaignMessagingService.sendCreateCampaignMessage(saved.getId(), delay);

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
        try {
            CampaignReservation reservation = buildReservation(
                    reserveTokensEvent,
                    CampaignReservation.ReservationStatus.PROCESSING
            );
            reservationRepository.save(reservation);
            reservationRepository.flush();
        } catch (DataIntegrityViolationException exception) {
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
                updateReservationStatus(reserveTokensEvent.transactionId(), CampaignReservation.ReservationStatus.RESERVED, null);

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
            updateReservationStatus(reserveTokensEvent.transactionId(), CampaignReservation.ReservationStatus.FAILED, e.getMessage());

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
        campaign.setCampaignStatus(Campaign.CampaignStatus.PENDING);
        return campaign;
    }

    private void resendReply(CampaignReservation reservation) {
        if (reservation.getStatus() == CampaignReservation.ReservationStatus.RESERVED) {
            campaignMessagingService.sendSagaReply(
                    RabbitMQConfig.ROUTING_SUCCESS,
                    new TokensReservedSuccessEvent(
                            reservation.getTransactionId(),
                            reservation.getCampaignId(),
                            reservation.getAmount()
                    )
            );
        } else if (reservation.getStatus() == CampaignReservation.ReservationStatus.FAILED) {
            campaignMessagingService.sendSagaReply(
                    RabbitMQConfig.ROUTING_FAILED,
                    new TokensReservedFailedEvent(
                            reservation.getTransactionId(),
                            reservation.getCampaignId(),
                            reservation.getAmount(),
                            reservation.getFailureReason()
                    )
            );
        }
    }

    private void updateReservationStatus(Long transactionId, CampaignReservation.ReservationStatus reservationStatus, String failureReason) {
        CampaignReservation reservation = reservationRepository.findByTransactionId(transactionId).orElseThrow(
                () -> new EntityNotFoundException("There is no reservation with id: " + transactionId)
        );
        reservation.setStatus(reservationStatus);
        reservation.setFailureReason(failureReason);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
    }

    private CampaignReservation buildReservation(ReserveTokensEvent reserveTokensEvent, CampaignReservation.ReservationStatus status) {
        CampaignReservation reservation = new CampaignReservation();
        reservation.setTransactionId(reserveTokensEvent.transactionId());
        reservation.setCampaignId(reserveTokensEvent.campaignId());
        reservation.setAmount(reserveTokensEvent.amount());
        reservation.setStatus(status);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());
        return reservation;
    }
}
