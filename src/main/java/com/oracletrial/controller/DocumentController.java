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
import java.util.Locale;

/**
 * Handles document upload requests.
 *
 * <p>Delegates all PDF processing to {@link DocumentService} and all
 * embedding/storage work to {@link EmbeddingService}, and stays focused on
 * request/response handling and input validation.</p>
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
     * Uploads a PDF, extracts and chunks its text, then embeds every chunk
     * and stores it in Qdrant so it can be searched later.
     *
     * @param file uploaded PDF
     * @return document name, how many chunks were created, and a status message
     * @throws IOException if the PDF cannot be read
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadPdf(@RequestParam("file") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a PDF file to upload");
        }

        String documentName = file.getOriginalFilename();
        if (!isPdf(file, documentName)) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        log.info("Received upload: {}", documentName);

        String rawText = documentService.extractText(file);
        String cleanedText = documentService.cleanText(rawText);

        if (cleanedText.isBlank()) {
            throw new IllegalArgumentException("No readable text found in " + documentName);
        }

        List<DocumentChunk> chunks = documentService.chunkText(documentName, cleanedText);
        log.info("Document {} split into {} chunks", documentName, chunks.size());

        for (DocumentChunk chunk : chunks) {
            embeddingService.save(chunk);
        }
        log.info("Stored embeddings for {} chunks of {} in Qdrant", chunks.size(), documentName);

        return new UploadResponse(documentName, chunks.size(), "Document processed successfully");
    }

    /**
     * Checks the file looks like a PDF, using either the declared content
     * type or the file extension, since browsers/Postman don't always send
     * a reliable content type for multipart uploads.
     */
    private boolean isPdf(MultipartFile file, String documentName) {
        boolean pdfContentType = MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType());
        boolean pdfExtension = documentName != null
                && documentName.toLowerCase(Locale.ROOT).endsWith(".pdf");
        return pdfContentType || pdfExtension;
    }
}
