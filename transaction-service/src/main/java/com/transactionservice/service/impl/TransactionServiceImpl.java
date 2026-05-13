package com.transactionservice.service.impl;

import com.transactionservice.config.RabbitMQConfig;
import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.CreateTransactionResponseDto;
import com.transactionservice.event.ReserveTokensEvent;
import com.transactionservice.event.TokensReservedFailedEvent;
import com.transactionservice.event.TokensReservedSuccessEvent;
import com.transactionservice.model.Status;
import com.transactionservice.model.Transaction;
import com.transactionservice.repository.TransactionRepository;
import com.transactionservice.service.TransactionMessagingService;
import com.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMessagingService transactionMessagingService;

    @Override
    public CreateTransactionResponseDto createTransaction(CreateTransactionRequestDto requestDto) {
        Transaction transaction = toModel(requestDto);
        Transaction saved = transactionRepository.save(transaction);

        ReserveTokensEvent event = new ReserveTokensEvent(
                saved.getId(),
                saved.getCampaignId(),
                saved.getAmount()
        );

        transactionMessagingService.convertAndSend(
                RabbitMQConfig.ROUTING_RESERVE,
                event
        );

        return toDto(saved, Collections.emptyList());
    }

    @Override
    public void handleSuccessSagaReply(TokensReservedSuccessEvent event) {
        transactionRepository.findById(event.transactionId()).ifPresent(transaction -> {
            transaction.setStatus(Status.COMPLETED);
            transactionRepository.save(transaction);
        });
    }

    @Override
    public void handleFailedSagaReply(TokensReservedFailedEvent event) {
        transactionRepository.findById(event.transactionId()).ifPresent(transaction -> {
            transaction.setStatus(Status.FAILED);
            transactionRepository.save(transaction);
            //trigger refund
        });
    }

    private Transaction toModel(CreateTransactionRequestDto requestDto) {
        Transaction transaction = new Transaction();
        transaction.setUserId(requestDto.userId());
        transaction.setCampaignId(requestDto.campaignId());
        transaction.setAmount(requestDto.amount());
        transaction.setStatus(Status.PENDING);
        return transaction;
    }

    private CreateTransactionResponseDto toDto(Transaction transaction, List<String> exceptions) {
        return new CreateTransactionResponseDto(transaction.getUserId(), transaction.getCampaignId(), transaction.getAmount(), transaction.getStatus(), exceptions);
    }
}
