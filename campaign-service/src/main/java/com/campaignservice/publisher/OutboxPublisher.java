package com.campaignservice.publisher;

import com.campaignservice.model.OutboxEvent;
import com.campaignservice.publisher.strategy.outbox.OutboxEventRegistry;
import com.campaignservice.publisher.strategy.outbox.OutboxEventStrategy;
import com.campaignservice.repository.OutboxEventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
@AllArgsConstructor
public class OutboxPublisher {
    public static final int RETRY_DELAY_SECONDS = 10;
    public static final int PUBLISH_INTERVAL = 1000;
    private static final int MAX_RETRY_COUNT = 3;

    private final OutboxEventRepository outboxRepository;
    private final OutboxEventRegistry outboxEventRegistry;

    @Scheduled(fixedDelay = PUBLISH_INTERVAL)
    public void publishOutboxEvent() {
        List<OutboxEvent> events = outboxRepository
                .findByStatusAndNextAttemptAtLessThan(
                        OutboxEvent.OutboxStatus.NEW,
                        Instant.now()
                );

        for (OutboxEvent event : events) {
            event.setStatus(OutboxEvent.OutboxStatus.PROCESSING);
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
                log.error("Failed to publish outbox event {}", event.getId(), e);
                outboxRepository.findById(event.getId()).ifPresent(outbox -> {
                    outbox.setStatus(OutboxEvent.OutboxStatus.NEW);
                    outboxRepository.save(outbox);
                });
            }

            correlationData.getFuture().whenComplete((confirm, ex) -> {
                Long outboxId = event.getId();

                OutboxEvent fresh = outboxRepository.findById(outboxId).orElseThrow();
                if (ex != null || (confirm != null && !confirm.isAck()) || correlationData.getReturned() != null) {
                    fresh.setRetryCount(fresh.getRetryCount() + 1);
                    if (fresh.getRetryCount() > MAX_RETRY_COUNT) {
                        fresh.setStatus(OutboxEvent.OutboxStatus.FAILED);
                    } else {
                        fresh.setStatus(OutboxEvent.OutboxStatus.NEW);
                    }
                    fresh.setNextAttemptAt(Instant.now().plusSeconds(RETRY_DELAY_SECONDS));

                    String reason = ex != null ? ex.getMessage() : confirm != null
                            ? confirm.getReason()
                            : "Unknown publish failure";
                    fresh.setLastError(reason);

                    outboxRepository.save(fresh);
                } else {
                    fresh.setStatus(OutboxEvent.OutboxStatus.SENT);
                    outboxRepository.save(fresh);
                }
            });
        }
    }
}
