package com.oracle.trial.model;

/**
 * Response returned to the frontend after a PDF has been processed.
 */
public record UploadResponse(
        String message,
        String documentName,
        int pageCount,
        int chunkCount
) {
}
