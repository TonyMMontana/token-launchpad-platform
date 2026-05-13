package com.campaignservice.repository;

import com.campaignservice.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @Modifying
    @Query(value = "UPDATE campaigns SET tokens_sold = COALESCE(tokens_sold, 0) + :amount " +
            "WHERE id = :campaignId " +
            "AND status = 'LIVE' " +
            "AND (target_amount - COALESCE(tokens_sold, 0)) >= :amount",
            nativeQuery = true)
    int reserveTokensAtomically(@Param("campaignId") Long campaignId,
                                @Param("amount") BigDecimal amount);
}