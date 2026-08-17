package com.oracletrial.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents one small piece of a document.
 *
 * <p>RAG systems never embed or search an entire document at once, because
 * a single vector cannot represent all the different topics in a large PDF.
 * Instead, every document is split into smaller chunks, and each chunk is
 * embedded and searched separately. This class holds one such chunk, plus
 * enough metadata to trace the chunk back to its original document so an
 * answer can cite exactly where the information came from.</p>
 */
@Getter
@AllArgsConstructor
public class DocumentChunk {

    /** The chunk's text content. */
    private final String text;

    /** Name of the document this chunk belongs to, e.g. "hr-policy.pdf". */
    private final String document;

    /** Position of this chunk within the document, starting at 1. */
    private final int chunk;

    /**
     * Page number the chunk came from, if known.
     *
     * <p>Left {@code null} for now because the current PDF extraction step
     * does not track page boundaries. The field is kept here so page
     * numbers can be filled in later without changing this class.</p>
     */
    private final Integer page;
}
