package com.oracle.trial.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * A single, simple place to turn any unhandled exception (a bad PDF, an
 * OpenAI/Qdrant call failing, etc.) into a readable JSON error instead of a
 * raw stack trace, so the React app can show a friendly message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage() == null ? "Something went wrong" : ex.getMessage()));
    }
}
