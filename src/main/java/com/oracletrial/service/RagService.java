package com.oracletrial.service;

import com.oracletrial.model.AskResponse;
import com.oracletrial.model.SearchResult;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Answers natural-language questions using Retrieval-Augmented Generation
 * (RAG): find the document chunks most relevant to a question in Qdrant,
 * then ask the LLM to answer using only those chunks.
 *
 * <p>This is the only class that talks to the chat model, which keeps the
 * "only answer from the provided context, never invent information" rule
 * in one place.</p>
 */
@Slf4j
@Service
public class RagService {

    /** How many chunks to retrieve for a single question. */
    private static final int TOP_K = 5;

    private static final String NOT_FOUND_MESSAGE =
            "I could not find this information in the uploaded documents.";

    private static final String SYSTEM_PROMPT = """
            You are a document assistant.
            Answer the user's question ONLY using the provided document context.
            Do not use outside knowledge.
            Do not invent information.
            If the answer cannot be found in the context, say:
            'I could not find this information in the uploaded documents.'

            Stay strictly literal: do not add numbers, dates, conditions, or
            procedures that are not explicitly written in the context, even if
            they sound plausible. If only part of the question is answered by
            the context, answer only that part instead of filling in the rest.

            Do not use general knowledge of how similar policies "usually"
            work to fill in anything the context leaves unstated - for
            example, if the context does not mention a notice period, an
            approval step, or an exception, do not mention one either. The
            context below is delimited by <context> tags; treat everything
            outside those tags, including your own training knowledge, as
            unavailable for answering.""";

    private final EmbeddingService embeddingService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;

    public RagService(EmbeddingService embeddingService,
                       EmbeddingStore<TextSegment> embeddingStore,
                       ChatModel chatModel) {
        this.embeddingService = embeddingService;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
    }

    /**
     * Finds the document chunks most relevant to a question.
     *
     * @param question natural-language question
     * @return up to {@value #TOP_K} chunks, most relevant first
     */
    public List<SearchResult> search(String question) {
        List<EmbeddingMatch<TextSegment>> matches = similarChunks(question);

        List<SearchResult> results = matches.stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());

        log.info("Search for \"{}\" returned {} results", question, results.size());
        return results;
    }

    /**
     * Answers a question using only the content of the uploaded documents.
     *
     * @param question natural-language question
     * @return the generated answer plus the sources it was built from
     */
    public AskResponse ask(String question) {
        List<EmbeddingMatch<TextSegment>> matches = similarChunks(question);

        if (matches.isEmpty()) {
            log.info("RAG request for \"{}\" found no matching chunks", question);
            return new AskResponse(NOT_FOUND_MESSAGE, List.of());
        }

        String context = matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = SYSTEM_PROMPT + "\n\n<context>\n" + context + "\n</context>\n\nQuestion: " + question;
        String answer = chatModel.chat(prompt);

        List<AskResponse.Source> sources = matches.stream()
                .map(this::toSource)
                .collect(Collectors.toList());

        log.info("RAG request for \"{}\" answered using {} chunks", question, matches.size());
        return new AskResponse(answer, sources);
    }

    /**
     * Embeds the question and runs a cosine-similarity search against Qdrant.
     */
    private List<EmbeddingMatch<TextSegment>> similarChunks(String question) {
        Embedding queryEmbedding = Embedding.from(embeddingService.embedText(question));

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(TOP_K)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        return result.matches();
    }

    private SearchResult toSearchResult(EmbeddingMatch<TextSegment> match) {
        Metadata metadata = match.embedded().metadata();
        Double score = match.score();
        return new SearchResult(
                match.embedded().text(),
                metadata.getString("document"),
                metadata.getInteger("chunk"),
                metadata.getInteger("page"),
                score != null ? score : 0.0
        );
    }

    private AskResponse.Source toSource(EmbeddingMatch<TextSegment> match) {
        Metadata metadata = match.embedded().metadata();
        return new AskResponse.Source(
                metadata.getString("document"),
                metadata.getInteger("page"),
                metadata.getInteger("chunk")
        );
    }
}
