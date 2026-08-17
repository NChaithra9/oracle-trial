package com.oracletrial.controller;

import com.oracletrial.model.AskRequest;
import com.oracletrial.model.AskResponse;
import com.oracletrial.model.SearchResult;
import com.oracletrial.service.RagService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Handles question-answering over the uploaded documents.
 *
 * <p>Both endpoints are backed by {@link RagService}: {@code /api/search}
 * returns the raw matching chunks, and {@code /api/ask} additionally asks
 * the LLM to turn those chunks into a direct, cited answer.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * Searches Qdrant for the chunks most relevant to a question.
     *
     * @param request question to search for
     * @return the top matching chunks, wrapped as {@code {"results": [...]}}
     */
    @PostMapping("/search")
    public Map<String, List<SearchResult>> search(@Valid @RequestBody AskRequest request) {
        log.info("Search requested: {}", request.getQuestion());
        List<SearchResult> results = ragService.search(request.getQuestion());
        return Map.of("results", results);
    }

    /**
     * Answers a question using Retrieval-Augmented Generation.
     *
     * @param request question to answer
     * @return the generated answer and the sources it came from
     */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        log.info("RAG question asked: {}", request.getQuestion());
        return ragService.ask(request.getQuestion());
    }
}
