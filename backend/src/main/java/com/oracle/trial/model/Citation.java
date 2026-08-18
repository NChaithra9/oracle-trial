package com.oracle.trial.model;

/**
 * A single source reference shown alongside an answer: which document and
 * page the supporting text came from, plus the actual retrieved chunk text
 * ("excerpt") so the UI can show exactly what the answer was based on.
 *
 * "excerpt" is always the real text of the Qdrant chunk that was used to
 * build the answer's context - never generated or guessed. When the same
 * document + page appears from more than one chunk, RagService keeps the
 * first (highest-ranked) chunk's text as the excerpt for that page.
 */
public record Citation(String documentName, int pageNumber, String excerpt) {
}
