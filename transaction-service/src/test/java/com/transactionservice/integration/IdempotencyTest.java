package com.transactionservice.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.exception.domain.IdempotencyConflictException;
import com.transactionservice.repository.OutboxRepository;
import com.transactionservice.repository.TransactionRepository;
import com.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public class IdempotencyTest extends AbstractIntegrationTest {

    public static final long CAMPAIGN_ID = 1L;
    public static final int SECONDS = 10;
    public static final int MILLIS = 200;

    @Autowired
    TransactionService transactionService;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Test
    public void shouldCreateOnlyOneTransactionAndOutboxForRepeatedSameRequest() {
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        CreateTransactionRequestDto requestDto = new CreateTransactionRequestDto(CAMPAIGN_ID, BigDecimal.TEN);

        for (int i = 0; i < 10; i++) {
            transactionService.createTransaction(userId, idempotencyKey, requestDto);
        }

        await()
                .atMost(Duration.ofSeconds(SECONDS))
                .pollInterval(Duration.ofMillis(MILLIS))
                .untilAsserted(() -> {
                    long transactionCount = transactionRepository.count();

                    assertEquals(
                            1,
                            transactionCount,
                            "Exactly one Transaction should be created for unique userId and idempotencyKey"
                    );

                    long outboxCount = outboxRepository.count();
                    assertEquals(
                            1,
                            outboxCount,
                            "Exactly one Outbox event should be created for unique userId and idempotencyKey"
                    );
                });
    }

    @Test
    public void shouldThrowIdempotencyConflictExceptionForDuplicateUserIdAndIdempotencyKeyButDifferentBody() {
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        CreateTransactionRequestDto requestDto = new CreateTransactionRequestDto(CAMPAIGN_ID, BigDecimal.TEN);
        CreateTransactionRequestDto changedRequestDto = new CreateTransactionRequestDto(CAMPAIGN_ID, BigDecimal.ONE);

        transactionService.createTransaction(userId, idempotencyKey, requestDto);

        assertThrows(
                IdempotencyConflictException.class,
                () -> transactionService.createTransaction(userId, idempotencyKey, changedRequestDto),
                "Should throw IdempotencyConflictException for duplicate userId and idempotencyKey and different body"
        );
    }

    @Test
    public void shouldCreateOutboxEventAndTransactionForUniqueUserIdsAndTheSameIdempotencyKey() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        CreateTransactionRequestDto requestDto = new CreateTransactionRequestDto(CAMPAIGN_ID, BigDecimal.TEN);

        transactionService.createTransaction(firstUserId, idempotencyKey, requestDto);
        transactionService.createTransaction(secondUserId, idempotencyKey, requestDto);

        await()
                .atMost(Duration.ofSeconds(SECONDS))
                .pollInterval(Duration.ofMillis(MILLIS))
                .untilAsserted(() -> {
                    long transactionCount = transactionRepository.count();

                    assertEquals(
                            2,
                            transactionCount,
                            "Transaction should be created for different users with the same idempotencyKey"
                    );

                    long outboxCount = outboxRepository.count();
                    assertEquals(
                            2,
                            outboxCount,
                            "Outbox events should be created for different users with the same idempotencyKey"
                    );
                });
    }
}
