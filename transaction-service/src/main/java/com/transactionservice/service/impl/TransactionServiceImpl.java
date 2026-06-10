package com.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.launchpad.common.event.ReserveTokensEvent;
import com.launchpad.common.event.TokensReservedFailedEvent;
import com.launchpad.common.event.TokensReservedSuccessEvent;
import com.launchpad.common.security.Roles;
import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.TransactionResponseDto;
import com.transactionservice.exception.domain.IdempotencyConflictException;
import com.transactionservice.model.outbox.OutboxEvent;
import com.transactionservice.model.outbox.OutboxStatus;
import com.transactionservice.model.outbox.OutboxType;
import com.transactionservice.model.transaction.Transaction;
import com.transactionservice.model.transaction.TransactionStatus;
import com.transactionservice.repository.OutboxRepository;
import com.transactionservice.repository.TransactionRepository;
import com.transactionservice.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransactionResponseDto createTransaction(UUID userId, UUID idempotencyKey, CreateTransactionRequestDto requestDto) {
        log.info("Creating transaction for userId: {} ", userId);
        LocalDateTime now = LocalDateTime.now();
        int updated = transactionRepository.insertIfAbsent(
                userId,
                idempotencyKey,
                requestDto.amount(),
                requestDto.campaignId(),
                now,
                now
        );

        if (updated == 0) {
            log.info("Idempotency conflict detected. Returning existing transaction for key: {} for user: {}", idempotencyKey, userId);

            Transaction existing = transactionRepository
                    .getTransactionByIdempotencyKeyAndUserId(idempotencyKey, userId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Critical: Transaction not found for userId: " + userId
                                    + " and idempotencyKey: " + idempotencyKey));

            if (!requestDto.campaignId().equals(existing.getCampaignId())
                    || requestDto.amount().compareTo(existing.getAmount()) != 0) {
                throw new IdempotencyConflictException("There is already a transaction with idempotencyKey: " + idempotencyKey);
            }

            return toDto(existing, Collections.emptyList());
        }

        Transaction saved = transactionRepository
                .getTransactionByIdempotencyKeyAndUserId(idempotencyKey, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Critical: Transaction not found for userId: " + userId
                                + " and idempotencyKey: " + idempotencyKey));

        OutboxEvent outboxEvent = createOutboxEvent(saved);
        outboxRepository.save(outboxEvent);

        return toDto(saved, Collections.emptyList());
    }

    @Override
    @Transactional
    public void handleSuccessSagaReply(TokensReservedSuccessEvent event) {
        log.info("Received SUCCESS saga reply for transaction: {} ", event.transactionId());

        int updated = transactionRepository.updateStatusIfPending(
                event.transactionId(),
                TransactionStatus.COMPLETED.name(),
                LocalDateTime.now()
        );

        if (updated == 0) {
            log.warn("Saga reply ignored. Transaction {} is not PENDING.", event.transactionId());
            return;
        }

        log.info("Transaction {} successfully marked as COMPLETED", event.transactionId());
    }

    @Override
    @Transactional
    public void handleFailedSagaReply(TokensReservedFailedEvent event) {
        log.info("Received FAILED saga reply for transaction: {} ", event.transactionId());

        int updated = transactionRepository.updateStatusIfPending(
                event.transactionId(),
                TransactionStatus.FAILED.name(),
                LocalDateTime.now()
        );

        if (updated == 0) {
            log.warn("FAILED Saga reply ignored. Transaction {} is not PENDING.", event.transactionId());
            return;
        }

        log.info("Transaction {} marked as FAILED.", event.transactionId());
        // TODO: trigger refund process
    }

    @Override
    public TransactionResponseDto getTransaction(Long transactionId, UUID userId, Set<String> roles) {
        Optional<Transaction> transactionOptional;

        if (roles.contains(Roles.ADMIN)) {
            transactionOptional = transactionRepository.findById(transactionId);
        } else {
            transactionOptional =
                    transactionRepository.findByIdAndUserId(transactionId, userId);
        }

        Transaction transaction = transactionOptional.orElseThrow(() ->
                new EntityNotFoundException(
                        "Transaction not found for transactionId: " + transactionId
                )
        );

        return toDto(transaction, Collections.emptyList());
    }

    @Override
    public Page<TransactionResponseDto> getTransactions(UUID userId, Pageable pageable) {
        return transactionRepository.findTransactionsByUserId(userId, pageable)
                .map(transaction -> toDto(transaction, Collections.emptyList()));
    }

    private OutboxEvent createOutboxEvent(Transaction saved) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType(OutboxType.RESERVE_TOKENS);
        outboxEvent.setAggregateId(saved.getId());
        outboxEvent.setStatus(OutboxStatus.NEW);
        outboxEvent.setRetryCount(0);
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEvent.setNextAttemptAt(LocalDateTime.now());

        ReserveTokensEvent payload = new ReserveTokensEvent(
                saved.getId(),
                saved.getCampaignId(),
                saved.getAmount()
        );
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        try {
            outboxEvent.setPayload(objectWriter.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Unable to write payload for outboxEvent for transaction {}", saved.getId(), e);
            throw new RuntimeException(e);
        }
        return outboxEvent;
    }

    private TransactionResponseDto toDto(Transaction transaction, List<String> exceptions) {
        return new TransactionResponseDto(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getCampaignId(),
                transaction.getAmount(),
                transaction.getTransactionStatus(),
                transaction.getCreatedAt(),
                exceptions
        );
    }
}
