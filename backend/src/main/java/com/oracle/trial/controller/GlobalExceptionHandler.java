package com.oracle.trial.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * A single, simple place to turn any unhandled exception (a bad PDF, an
 * OpenAI/Qdrant call failing, etc.) into a readable JSON error instead of a
 * raw stack trace, so the React app can show a friendly message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Services throw Spring's built-in ResponseStatusException when they
     * already know the right HTTP status for a specific problem - e.g. a 404
     * when deleting a document that doesn't exist. Handled separately so
     * that status isn't flattened into a generic 500 by the handler below.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage() == null ? "Something went wrong" : ex.getMessage()));
    }
}
