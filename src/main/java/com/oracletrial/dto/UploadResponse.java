package com.oracletrial.dto;

import com.oracletrial.model.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Response returned by the document upload endpoint.
 *
 * <p>Shows the chunks generated from an uploaded document, plus a summary
 * of the embeddings created for them, so the pipeline can be inspected
 * directly in Postman before Qdrant storage is added in a later
 * milestone.</p>
 */
@Getter
@AllArgsConstructor
public class UploadResponse {

    /** Name of the uploaded document. */
    private final String document;

    /** How many chunks the document was split into. */
    private final int chunkCount;

    /** Size of each embedding vector, e.g. 1536. Same for every chunk. */
    private final int embeddingDimension;

    /** Chunks generated from the document's cleaned text. */
    private final List<DocumentChunk> chunks;
}
