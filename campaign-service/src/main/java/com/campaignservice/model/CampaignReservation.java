package com.campaignservice.model;

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
import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "campaign_reservations",
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_id", columnNames = {"transaction_id"})
})
public class CampaignReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(unique = true, nullable = false)
    Long transactionId;
    Long campaignId;
    BigDecimal amount;
    @Enumerated(EnumType.STRING)
    ReservationStatus status;
    String failureReason;
    Instant createdAt;
    Instant updatedAt;

    public enum ReservationStatus {
        PROCESSING,
        RESERVED,
        FAILED
    }
}
