package com.transactionservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.CreateTransactionResponseDto;
import com.transactionservice.event.messaging.ReserveTokensEvent;
import com.transactionservice.event.messaging.TokensReservedFailedEvent;
import com.transactionservice.event.messaging.TokensReservedSuccessEvent;
import com.transactionservice.model.outbox.OutboxEvent;
import com.transactionservice.model.outbox.OutboxStatus;
import com.transactionservice.model.outbox.OutboxType;
import com.transactionservice.model.transaction.Transaction;
import com.transactionservice.model.transaction.TransactionStatus;
import com.transactionservice.repository.OutboxRepository;
import com.transactionservice.repository.TransactionRepository;
import com.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
    public CreateTransactionResponseDto createTransaction(UUID userId, CreateTransactionRequestDto requestDto) {
        log.info("Creating transaction for userId: {} ",userId);
        Transaction transaction = toModel(userId, requestDto);
        transaction.setTransactionStatus(TransactionStatus.PENDING);

        Transaction saved = transactionRepository.save(transaction);

        OutboxEvent outboxEvent = createOutboxEvent(saved);
        outboxRepository.save(outboxEvent);

        return toDto(saved, Collections.emptyList());
    }

    @Override
    public void handleSuccessSagaReply(TokensReservedSuccessEvent event) {
        transactionRepository.findById(event.transactionId()).ifPresent(transaction -> {
            transaction.setTransactionStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
        });
    }

    @Override
    public void handleFailedSagaReply(TokensReservedFailedEvent event) {
        transactionRepository.findById(event.transactionId()).ifPresent(transaction -> {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            //trigger refund
        });
    }

    private Transaction toModel(UUID userId, CreateTransactionRequestDto requestDto) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setCampaignId(requestDto.campaignId());
        transaction.setAmount(requestDto.amount());
        return transaction;
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

    private CreateTransactionResponseDto toDto(Transaction transaction, List<String> exceptions) {
        return new CreateTransactionResponseDto(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getCampaignId(),
                transaction.getAmount(),
                transaction.getTransactionStatus(),
                exceptions
        );
    }
}
