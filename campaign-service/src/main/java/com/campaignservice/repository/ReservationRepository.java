package com.campaignservice.repository;

import com.campaignservice.model.CampaignReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<CampaignReservation, Long> {
    Optional<CampaignReservation> findByTransactionId(Long transactionId);
}
