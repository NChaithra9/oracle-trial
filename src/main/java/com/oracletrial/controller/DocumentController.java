package com.oracletrial.controller;

import com.oracletrial.dto.UploadResponse;
import com.oracletrial.model.DocumentChunk;
import com.oracletrial.service.DocumentService;
import com.oracletrial.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Handles document upload requests.
 *
 * <p>Delegates all PDF processing to {@link DocumentService} and all
 * embedding generation to {@link EmbeddingService}, and stays focused on
 * request/response handling only.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final EmbeddingService embeddingService;

    public DocumentController(DocumentService documentService, EmbeddingService embeddingService) {
        this.documentService = documentService;
        this.embeddingService = embeddingService;
    }

    /**
     * Uploads a PDF, chunks it, and generates an embedding for every chunk.
     *
     * <p>Runs the document through extraction, cleaning, chunking, and
     * embedding so the result can be inspected in Postman. Storing the
     * embeddings in Qdrant is added in a later milestone - for now the
     * vectors are generated but not kept anywhere.</p>
     *
     * @param file uploaded PDF
     * @return document name, chunk/embedding summary, and the chunks themselves
     * @throws IOException if the PDF cannot be read
     */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public UploadResponse uploadDocument(
            @RequestParam("file") MultipartFile file) throws IOException {

        String documentName = file.getOriginalFilename();
        log.info("Received upload: {}", documentName);

        String rawText = documentService.extractText(file);
        String cleanedText = documentService.cleanText(rawText);
        List<DocumentChunk> chunks = documentService.chunkText(documentName, cleanedText);
        log.info("Document {} split into {} chunks", documentName, chunks.size());

        int embeddingDimension = 0;
        for (DocumentChunk chunk : chunks) {
            float[] vector = embeddingService.embedText(chunk.getText());
            embeddingDimension = vector.length;
        }
        log.info("Generated embeddings for {} chunks (dimension={})", chunks.size(), embeddingDimension);

        return new UploadResponse(documentName, chunks.size(), embeddingDimension, chunks);
    }
}