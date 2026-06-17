package com.campaignservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long aggregateId;

    @Enumerated(EnumType.STRING)
    EventType eventType;

    @Enumerated(EnumType.STRING)
    OutboxStatus status;

    String payload;

    int retryCount;

    Instant nextAttemptAt;

    String lastError;

    Instant createdAt;

    Instant setNextAttemptAt;

    public enum EventType {
        RESERVE_TOKENS_SUCCESS,
        RESERVE_TOKENS_FAILURE
    }

    public enum OutboxStatus {
        NEW,
        PROCESSING,
        SENT,
        FAILED
    }
}
