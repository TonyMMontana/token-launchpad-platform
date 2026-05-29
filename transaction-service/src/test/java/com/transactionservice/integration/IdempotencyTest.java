package com.transactionservice.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.launchpad.common.event.TokensReservedFailedEvent;
import com.launchpad.common.event.TokensReservedSuccessEvent;
import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.exception.domain.IdempotencyConflictException;
import com.transactionservice.model.transaction.Transaction;
import com.transactionservice.model.transaction.TransactionStatus;
import com.transactionservice.repository.OutboxRepository;
import com.transactionservice.repository.TransactionRepository;
import com.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IdempotencyTest extends AbstractIntegrationTest {

    public static final long CAMPAIGN_ID = 1L;
    public static final int SECONDS = 10;
    public static final int MILLIS = 200;
    private static final int THREAD_COUNT = 10;
    public static final int TIMEOUT = 10;
    private static final int COUNT = 1;

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

    @Test
    public void shouldUpdateStatusForSuccessSagaReplyWhenTransactionStatusIsPending() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        transactionRepository.insertIfAbsent(
                userId,
                idempotencyKey,
                BigDecimal.TEN,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        Transaction transaction = transactionRepository.getTransactionByIdempotencyKeyAndUserId(idempotencyKey, userId).orElseThrow();
        TokensReservedSuccessEvent event = new TokensReservedSuccessEvent(transaction.getId(), 1L, BigDecimal.TEN);

        int threadCount = THREAD_COUNT;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(COUNT);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    countDownLatch.await();
                    transactionService.handleSuccessSagaReply(event);
                } catch (InterruptedException ignored) {
                }
            });
        }
        countDownLatch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(TIMEOUT, TimeUnit.SECONDS);

        transaction = transactionRepository.getTransactionByIdempotencyKeyAndUserId(idempotencyKey, userId).orElseThrow();
        assertEquals(
                TransactionStatus.COMPLETED,
                transaction.getTransactionStatus(),
                "Transaction status should be COMPLETED"
        );
    }

    @Test
    public void shouldNotUpdateStatusForAnySagaReplyWhenTransactionStatusIsNotPending() {
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        transactionRepository.insertIfAbsent(
                userId,
                idempotencyKey,
                BigDecimal.TEN,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        Transaction transaction = transactionRepository.getTransactionByIdempotencyKeyAndUserId(idempotencyKey, userId).orElseThrow();
        TokensReservedFailedEvent event = new TokensReservedFailedEvent(transaction.getId(), 1L, BigDecimal.TEN, "Test reason");

        for (int i = 0; i < 5; i++) {
            transactionService.handleFailedSagaReply(event);
        }

        transaction = transactionRepository.getTransactionByIdempotencyKeyAndUserId(idempotencyKey, userId).orElseThrow();
        assertEquals(
                TransactionStatus.FAILED,
                transaction.getTransactionStatus(),
                "Transaction status should be FAILED"
        );

        TokensReservedSuccessEvent conflictEvent = new TokensReservedSuccessEvent(transaction.getId(), 1L, BigDecimal.TEN);
        transactionService.handleSuccessSagaReply(conflictEvent);

        transaction = transactionRepository.getTransactionByIdempotencyKeyAndUserId(idempotencyKey, userId).orElseThrow();
        assertEquals(
                TransactionStatus.FAILED,
                transaction.getTransactionStatus(),
                "Transaction status should be FAILED"
        );
    }
}
