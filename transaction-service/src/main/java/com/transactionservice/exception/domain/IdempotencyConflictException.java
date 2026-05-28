package com.transactionservice.exception.domain;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super();
    }

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
