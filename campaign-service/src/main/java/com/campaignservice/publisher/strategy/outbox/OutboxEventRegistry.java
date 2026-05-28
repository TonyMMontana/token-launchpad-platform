package com.campaignservice.publisher.strategy.outbox;

import com.campaignservice.model.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OutboxEventRegistry {

    private final Map<OutboxEvent.EventType, OutboxEventStrategy> strategyMap;

    public OutboxEventRegistry(List<OutboxEventStrategy> strategyList) {
        this.strategyMap = strategyList.stream()
                .collect(Collectors.toMap(
                        OutboxEventStrategy::getOutboxEventStrategy,
                        strategy -> strategy
                ));
    }

    public OutboxEventStrategy getStrategy(OutboxEvent.EventType outboxType) {
        return Optional.ofNullable(strategyMap.get(outboxType))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No outbox strategy registered for type: " + outboxType
                ));
    }
}
