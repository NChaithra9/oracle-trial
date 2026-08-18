package com.oracle.trial.service;

import com.oracle.trial.model.AnswerResponse;
import com.oracle.trial.model.Citation;
import com.oracle.trial.model.DocumentSummary;
import com.oracle.trial.model.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Ties every step of the RAG pipeline together:
 *
 * Upload:   PDF -> extract text per page -> chunk -> embed -> store in Qdrant
 * Question: question -> embed -> similarity search in Qdrant -> build context
 *           -> ask OpenAI to answer -> return answer + citations
 */
@Service
public class RagService {

    private final PdfService pdfService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final AnswerService answerService;

    private final int chunkSize;
    private final int chunkOverlap;
    private final int topK;
    private final double relevanceRatio;

    public RagService(
            PdfService pdfService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            AnswerService answerService,
            @Value("${app.chunk.size}") int chunkSize,
            @Value("${app.chunk.overlap}") int chunkOverlap,
            @Value("${app.top-k}") int topK,
            @Value("${app.relevance-ratio}") double relevanceRatio
    ) {
        this.pdfService = pdfService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.answerService = answerService;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.topK = topK;
        this.relevanceRatio = relevanceRatio;
    }

    public UploadResponse processUpload(MultipartFile file) throws IOException {
        String documentName = file.getOriginalFilename();
        byte[] fileBytes = file.getBytes();
        String fileHash = sha256Hex(fileBytes);

        // Same file content uploaded before (regardless of what it's named
        // this time)? Skip re-chunking, re-embedding, and re-storing it -
        // that would just waste OpenAI API calls and create duplicate chunks.
        long alreadyStoredChunks = qdrantService.countByPayloadField("fileHash", fileHash);
        if (alreadyStoredChunks > 0) {
            return new UploadResponse(
                    "This exact document was already uploaded before - skipped re-processing.",
                    documentName,
                    0,
                    (int) alreadyStoredChunks,
                    true
            );
        }

        Map<Integer, String> pageTextByNumber = pdfService.extractTextByPage(fileBytes);

        int chunkCount = 0;
        for (Map.Entry<Integer, String> pageEntry : pageTextByNumber.entrySet()) {
            int pageNumber = pageEntry.getKey();
            List<String> chunks = chunkingService.chunkText(pageEntry.getValue(), chunkSize, chunkOverlap);

            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                String chunkText = chunks.get(chunkIndex);
                if (chunkText.isBlank()) {
                    continue;
                }

                List<Float> vector = embeddingService.embed(chunkText);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("documentName", documentName);
                payload.put("pageNumber", pageNumber);
                payload.put("chunkIndex", chunkIndex);
                payload.put("text", chunkText);
                payload.put("fileHash", fileHash);

                qdrantService.upsertPoint(UUID.randomUUID().toString(), vector, payload);
                chunkCount++;
            }
        }

        return new UploadResponse(
                "Document processed successfully",
                documentName,
                pageTextByNumber.size(),
                chunkCount,
                false
        );
    }

    /**
     * Rebuilds the list of already-uploaded documents directly from what's
     * stored in Qdrant, so it's accurate even after the backend restarts or
     * the browser page is refreshed - nothing is kept only in memory.
     */
    public List<DocumentSummary> listDocuments() {
        List<Map<String, Object>> payloads = qdrantService.scrollAllPayloads();

        Map<String, Set<Integer>> pageNumbersByDocument = new LinkedHashMap<>();
        Map<String, Integer> chunkCountByDocument = new LinkedHashMap<>();

        for (Map<String, Object> payload : payloads) {
            Object documentNameObj = payload.get("documentName");
            Object pageNumberObj = payload.get("pageNumber");
            if (documentNameObj == null || pageNumberObj == null) {
                continue;
            }
            String documentName = String.valueOf(documentNameObj);
            int pageNumber = ((Number) pageNumberObj).intValue();

            pageNumbersByDocument.computeIfAbsent(documentName, key -> new TreeSet<>()).add(pageNumber);
            chunkCountByDocument.merge(documentName, 1, Integer::sum);
        }

        List<DocumentSummary> summaries = new ArrayList<>();
        for (String documentName : pageNumbersByDocument.keySet()) {
            summaries.add(new DocumentSummary(
                    documentName,
                    pageNumbersByDocument.get(documentName).size(),
                    chunkCountByDocument.get(documentName)
            ));
        }
        return summaries;
    }

    @SuppressWarnings("unchecked")
    public AnswerResponse answerQuestion(String question) {
        List<Float> questionVector = embeddingService.embed(question);
        List<Map<String, Object>> results = qdrantService.search(questionVector, topK);

        if (results.isEmpty()) {
            return new AnswerResponse(AnswerService.NOT_FOUND_MESSAGE, List.of());
        }

        // Qdrant returns up to topK chunks ranked by score, but not every one
        // of them is actually relevant to this specific question - only the
        // ones close to the best match. Dropping the weak ones here keeps
        // both the generated answer's context and its citations honest.
        List<Map<String, Object>> relevantResults = filterByRelevance(results);

        StringBuilder context = new StringBuilder();
        Set<Citation> citations = new LinkedHashSet<>();

        for (Map<String, Object> result : relevantResults) {
            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            if (payload == null) {
                continue;
            }

            String documentName = String.valueOf(payload.get("documentName"));
            int pageNumber = ((Number) payload.get("pageNumber")).intValue();
            String text = String.valueOf(payload.get("text"));

            context.append("[Source: ").append(documentName)
                    .append(", page ").append(pageNumber).append("]\n")
                    .append(text).append("\n\n");

            citations.add(new Citation(documentName, pageNumber));
        }

        String answer = answerService.generateAnswer(question, context.toString());

        // If the model reports the answer wasn't found, don't attach citations
        // that would otherwise wrongly imply supporting evidence was found.
        if (answer.equalsIgnoreCase(AnswerService.NOT_FOUND_MESSAGE)) {
            return new AnswerResponse(AnswerService.NOT_FOUND_MESSAGE, List.of());
        }

        return new AnswerResponse(answer, new ArrayList<>(citations));
    }

    /**
     * Qdrant already sorts results by score, best first. Instead of a fixed
     * absolute cutoff (hard to pick well - similarity scores shift depending
     * on how similar in tone/domain your documents are), we keep only chunks
     * scoring within {@code relevanceRatio} of the best match. A weak second
     * or third match - like an unrelated policy chunk that only shares
     * generic wording with the question - naturally falls well below the
     * top score and gets dropped.
     */
    private List<Map<String, Object>> filterByRelevance(List<Map<String, Object>> results) {
        double topScore = scoreOf(results.get(0));
        double minScore = topScore * relevanceRatio;

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> result : results) {
            if (scoreOf(result) >= minScore) {
                filtered.add(result);
            }
        }
        return filtered;
    }

    private double scoreOf(Map<String, Object> result) {
        Object score = result.get("score");
        return score == null ? 0.0 : ((Number) score).doubleValue();
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every standard JVM.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
