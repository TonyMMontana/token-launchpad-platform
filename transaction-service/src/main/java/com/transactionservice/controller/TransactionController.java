package com.transactionservice.controller;

import static com.launchpad.common.header.InternalHeaders.IDEMPOTENCY_KEY_HEADER;
import static com.launchpad.common.header.InternalHeaders.USER_ID_HEADER;
import static com.launchpad.common.header.InternalHeaders.USER_ROLES_HEADER;

import com.transactionservice.dto.CreateTransactionRequestDto;
import com.transactionservice.dto.TransactionResponseDto;
import com.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponseDto> addTransaction(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) UUID idempotencyKey,
            @RequestBody CreateTransactionRequestDto requestDto) {

        return new ResponseEntity<>(
                transactionService.createTransaction(userId, idempotencyKey, requestDto),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> getTransaction(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @RequestHeader(USER_ROLES_HEADER) String rolesHeader,
            @PathVariable Long transactionId) {

        return ResponseEntity.ok(
                transactionService.getTransaction(
                        transactionId,
                        userId,
                        extractRoles(rolesHeader)
                )
        );
    }

    @GetMapping()
    public ResponseEntity<Page<TransactionResponseDto>> getUserTransactions(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        return ResponseEntity.ok(
                transactionService.getTransactions(
                        userId,
                        pageable
                )
        );
    }

    private Set<String> extractRoles(String roles) {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toSet());
    }
}
