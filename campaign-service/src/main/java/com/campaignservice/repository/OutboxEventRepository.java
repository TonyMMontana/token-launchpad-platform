package com.campaignservice.repository;

import com.campaignservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    Optional<OutboxEvent> findByAggregateId(Long aggregateId);

    List<OutboxEvent> findByStatusAndNextAttemptAtLessThan(OutboxEvent.OutboxStatus status, LocalDateTime now);
}
