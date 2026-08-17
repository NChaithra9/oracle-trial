package com.oracletrial.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response returned by the document upload endpoint.
 *
 * <p>Confirms that a document was processed successfully and shows how
 * many chunks it was split into. The chunk text itself is not echoed back
 * here - it now lives in Qdrant, where {@code /api/search} and
 * {@code /api/ask} can retrieve it.</p>
 */
@Getter
@AllArgsConstructor
public class UploadResponse {

    /** Name of the uploaded document. */
    private final String document;

    /** How many chunks the document was split into and stored in Qdrant. */
    private final int chunks;

    /** Human-readable status message. */
    private final String message;
}
