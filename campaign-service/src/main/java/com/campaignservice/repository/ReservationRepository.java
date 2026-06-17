package com.campaignservice.repository;

import com.campaignservice.model.CampaignReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<CampaignReservation, Long> {
    Optional<CampaignReservation> findByTransactionId(Long transactionId);

    @Modifying
    @Query(value = """
            INSERT INTO campaign_reservations
                (transaction_id, campaign_id, amount, status, created_at, updated_at)
            VALUES
                (:transactionId, :campaignId, :amount, 'PROCESSING', :createdAt, :updatedAt)
            ON CONFLICT (transaction_id) DO NOTHING
            """, nativeQuery = true)
    int claimProcessingReservation(@Param("transactionId") Long transactionId,
                                   @Param("campaignId") Long campaignId,
                                   @Param("amount") BigDecimal amount,
                                   @Param("createdAt") Instant createdAt,
                                   @Param("updatedAt") Instant updatedAt);
}
