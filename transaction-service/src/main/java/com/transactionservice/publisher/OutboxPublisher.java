package com.transactionservice.publisher;

import com.transactionservice.model.outbox.OutboxEvent;
import com.transactionservice.model.outbox.OutboxStatus;
import com.transactionservice.publisher.strategy.outbox.OutboxEventRegistry;
import com.transactionservice.publisher.strategy.outbox.OutboxEventStrategy;
import com.transactionservice.repository.OutboxRepository;
import com.transactionservice.service.TransactionMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    public static final int RETRY_DELAY_SECONDS = 10;
    public static final int PUBLISH_INTERVAL = 1000;
    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxRepository outboxRepository;
    private final OutboxEventRegistry outboxEventRegistry;
    private final TransactionMessagingService transactionMessagingService;

    @Scheduled(fixedDelay = PUBLISH_INTERVAL)
    public void publishOutbox() {
        List<OutboxEvent> events = outboxRepository
                .findByStatusInAndNextAttemptAtLessThan(
                        List.of(OutboxStatus.NEW),
                        LocalDateTime.now()
                );

        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
            outboxRepository.save(event);

            CorrelationData correlationData = new CorrelationData(event.getId().toString());

            try {
                log.info(
                        "Publishing OutboxId={} eventType={}",
                        event.getId(),
                        event.getEventType()
                );

                OutboxEventStrategy strategy = outboxEventRegistry.getStrategy(event.getEventType());
                strategy.publish(event, correlationData);
            } catch (Exception e) {
                log.error("Failed to publish reserve tokens reserveTokensEvent", e);
                outboxRepository.findById(event.getId()).ifPresent(outbox -> {
                    outbox.setStatus(OutboxStatus.NEW);
                    outboxRepository.save(outbox);
                });
            }

            correlationData.getFuture().whenComplete((confirm, ex) -> {
                Long outboxId = event.getId();

                OutboxEvent fresh = outboxRepository.findById(outboxId).orElseThrow();
                if (ex != null || (confirm != null && !confirm.isAck()) || correlationData.getReturned() != null) {
                    fresh.setRetryCount(fresh.getRetryCount() + 1);
                    if (fresh.getRetryCount() > MAX_RETRY_COUNT) {
                        fresh.setStatus(OutboxStatus.FAILED);
                    } else {
                        fresh.setStatus(OutboxStatus.NEW);
                    }
                    fresh.setNextAttemptAt(LocalDateTime.now().plusSeconds(RETRY_DELAY_SECONDS));

                    String reason = ex != null ? ex.getMessage() : confirm != null
                            ? confirm.getReason()
                            : "Unknown publish failure";
                    fresh.setLastError(reason);

                    outboxRepository.save(fresh);
                } else {
                    fresh.setStatus(OutboxStatus.SENT);
                    outboxRepository.save(fresh);
                }
            });
        }
    }
}
