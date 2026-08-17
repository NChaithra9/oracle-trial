package com.oracle.trial.controller;

import com.oracle.trial.model.AnswerResponse;
import com.oracle.trial.model.QuestionRequest;
import com.oracle.trial.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles user questions: embeds the question, searches Qdrant for the most
 * relevant chunks, and returns an AI-generated answer with citations.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final RagService ragService;

    public QuestionController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AnswerResponse> ask(@RequestBody QuestionRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AnswerResponse("Please enter a question.", java.util.List.of()));
        }

        AnswerResponse response = ragService.answerQuestion(request.question());
        return ResponseEntity.ok(response);
    }
}
