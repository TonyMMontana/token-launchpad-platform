package com.transactionservice.model.outbox;

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

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long aggregateId;

    @Enumerated(EnumType.STRING)
    OutboxType eventType;

    String payload;

    @Enumerated(EnumType.STRING)
    OutboxStatus status;

    int retryCount;

    LocalDateTime nextAttemptAt;

    String lastError;

    LocalDateTime createdAt;

    LocalDateTime publishedAt;
}
