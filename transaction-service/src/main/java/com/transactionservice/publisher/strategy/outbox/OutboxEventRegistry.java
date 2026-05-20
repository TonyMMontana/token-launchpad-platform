package com.transactionservice.publisher.strategy.outbox;

import com.transactionservice.model.outbox.OutboxType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OutboxEventRegistry {

    private final Map<OutboxType, OutboxEventStrategy> strategyMap;

    public OutboxEventRegistry(List<OutboxEventStrategy> strategyList) {
        this.strategyMap = strategyList.stream()
                .collect(Collectors.toMap(
                        OutboxEventStrategy::getOutboxEventStrategy,
                        strategy -> strategy
                ));
    }

    public OutboxEventStrategy getStrategy(OutboxType outboxType) {
        return Optional.ofNullable(strategyMap.get(outboxType))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No outbox strategy registered for type: " + outboxType
                ));
    }
}
