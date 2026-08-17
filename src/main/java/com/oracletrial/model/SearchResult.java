package com.oracletrial.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * One chunk returned by a similarity search against Qdrant.
 *
 * <p>Combines the chunk's text with citation metadata and a similarity
 * score, so {@code /api/search} can show exactly which document, page, and
 * chunk a result came from, and how close a match it was.</p>
 */
@Getter
@AllArgsConstructor
public class SearchResult {

    /** Matching chunk's text. */
    private final String text;

    /** Name of the source document, e.g. "hr-policy.pdf". */
    private final String document;

    /** Chunk number within the document, starting at 1. */
    private final int chunk;

    /** Page number the chunk came from, or {@code null} if unknown. */
    private final Integer page;

    /** Cosine similarity between the question and this chunk (0-1, higher means closer). */
    private final double score;
}
