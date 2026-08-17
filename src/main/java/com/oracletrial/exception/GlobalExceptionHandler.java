package com.oracletrial.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions from anywhere in the app into clean, consistent JSON
 * error responses instead of leaking stack traces to API callers.
 *
 * <p>Kept as a single handler for the whole application, since every error
 * case here (bad upload, bad question, PDF/AI/Qdrant failure) only needs a
 * status code and a short message - nothing fancier.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Invalid input: missing file, empty file, non-PDF file, blank question, etc. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(IllegalArgumentException e) {
        log.warn("Rejected request: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Bean Validation failures on @Valid request bodies, e.g. a missing "question" field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Invalid request");
        log.warn("Validation failed: {}", message);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** Uploaded file is larger than the configured multipart limit. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTooLarge(MaxUploadSizeExceededException e) {
        log.warn("Upload rejected: file too large");
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large");
    }

    /** Problems reading/parsing the PDF itself. */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handlePdfError(IOException e) {
        log.error("Failed to process PDF", e);
        return error(HttpStatus.BAD_REQUEST, "Could not read the uploaded PDF");
    }

    /**
     * Requests for a static file that doesn't exist, e.g. a browser's automatic
     * "/favicon.ico" request. This is routine, not an application error, so it's
     * logged quietly and returns a plain 404 instead of the generic 500 message.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMissingStaticResource(NoResourceFoundException e) {
        log.debug("No static resource for {}", e.getResourcePath());
        return error(HttpStatus.NOT_FOUND, "Not found");
    }

    /** Anything else: AI provider errors, Qdrant connectivity errors, and other unexpected failures. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong while processing your request");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
