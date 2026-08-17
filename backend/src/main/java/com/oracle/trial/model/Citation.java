package com.oracle.trial.model;

/**
 * A single source reference shown alongside an answer: which document and
 * which page the supporting text came from. Implemented as a record so that
 * duplicate citations (same document + page, from multiple chunks) are
 * automatically de-duplicated when stored in a Set.
 */
public record Citation(String documentName, int pageNumber) {
}
