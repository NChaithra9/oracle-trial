package com.oracle.trial.model;

import java.util.List;

/**
 * Response returned to the frontend after a question has been answered.
 * "citations" is empty when no relevant information was found.
 */
public record AnswerResponse(
        String answer,
        List<Citation> citations
) {
}
