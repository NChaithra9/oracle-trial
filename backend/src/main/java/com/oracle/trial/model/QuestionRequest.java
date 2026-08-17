package com.oracle.trial.model;

/**
 * Request body sent by the frontend when the user asks a question.
 */
public record QuestionRequest(String question) {
}
