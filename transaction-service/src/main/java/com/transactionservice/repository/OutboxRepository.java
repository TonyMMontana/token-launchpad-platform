package com.transactionservice.repository;

import com.transactionservice.model.outbox.OutboxEvent;
import com.transactionservice.model.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusInAndNextAttemptAtLessThan(List<OutboxStatus> statuses, LocalDateTime now);
}
