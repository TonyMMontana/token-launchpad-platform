package com.transactionservice.model.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_key_and_user_id", columnNames = {"user_id", "idempotency_key"})
        })
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long campaignId;

    UUID userId;

    BigDecimal amount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    TransactionStatus transactionStatus;

    UUID idempotencyKey;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
