package com.transactionservice.repository;

import com.transactionservice.model.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO transactions
                (user_id, idempotency_key, amount, campaign_id, status, created_at, updated_at)
            VALUES
                (:userId, :idempotencyKey, :amount, :campaignId, 'PENDING', :createdAt, :updatedAt)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId,
                       @Param("idempotencyKey") UUID idempotencyKey,
                       @Param("amount") BigDecimal amount,
                       @Param("campaignId") Long campaignId,
                       @Param("createdAt") LocalDateTime createdAt,
                       @Param("updatedAt") LocalDateTime updatedAt
    );

    Optional<Transaction> getTransactionByIdempotencyKeyAndUserId(UUID idempotencyKey, UUID userId);
}
