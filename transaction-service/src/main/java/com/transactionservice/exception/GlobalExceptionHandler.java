package com.transactionservice.exception;

import com.transactionservice.exception.domain.IdempotencyConflictException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    public static final String TIMESTAMP = "timestamp";
    public static final String STATUS = "status";
    public static final String ERRORS = "errors";

    @ExceptionHandler(IdempotencyConflictException.class)
    protected ResponseEntity<Object> handleIdempotencyConflict(Exception ex) {
        return handleException(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(Exception ex) {
        return handleException(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private ResponseEntity<Object> handleException(HttpStatus status, Object errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, status.value());
        body.put(ERRORS, errors);
        return new ResponseEntity<>(body, status);
    }
}
