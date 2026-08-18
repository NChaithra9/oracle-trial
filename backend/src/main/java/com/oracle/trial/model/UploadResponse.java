package com.oracle.trial.model;

/**
 * Response returned to the frontend after a PDF has been processed.
 * "duplicate" is true when this exact file (by content hash) was already
 * uploaded before, in which case it was NOT re-chunked or re-embedded.
 */
public record UploadResponse(
        String message,
        String documentName,
        int pageCount,
        int chunkCount,
        boolean duplicate
) {
}
