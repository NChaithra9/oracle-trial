package com.oracle.trial.model;

/**
 * Summary of one already-uploaded document, used to render the sidebar list
 * of documents The Oracle currently has indexed. Rebuilt from what's
 * actually stored in Qdrant, so it stays accurate even after a page reload.
 */
public record DocumentSummary(
        String documentName,
        int pageCount,
        int chunkCount
) {
}
