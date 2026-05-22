package com.transactionservice.controller;

import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.CreateTransactionResponseDto;
import com.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/transactions")
@RequiredArgsConstructor
public class TransactionController {
    public static final String USER_ID_HEADER = "X-User-Id";

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<CreateTransactionResponseDto> addTransaction(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestBody CreateTransactionRequestDto requestDto) {

        return new ResponseEntity<>(
                transactionService.createTransaction(userId, requestDto),
                HttpStatus.CREATED
        );
    }
}
