package com.oracle.trial.controller;

import com.oracle.trial.model.DocumentSummary;
import com.oracle.trial.model.UploadResponse;
import com.oracle.trial.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Handles PDF uploads and listing already-uploaded documents. The frontend
 * supports uploading 5+ PDFs simply by calling /upload once per file; every
 * document is added to the same Qdrant collection, tagged with its own
 * document name and page numbers.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final RagService ragService;

    public DocumentController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("File is empty", file.getOriginalFilename(), 0, 0, false));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("Only PDF files are supported", filename, 0, 0, false));
        }

        UploadResponse response = ragService.processUpload(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists every document currently indexed in Qdrant, so the frontend can
     * show an up-to-date sidebar that survives a page refresh (it isn't just
     * kept in React state).
     */
    @GetMapping
    public ResponseEntity<List<DocumentSummary>> list() {
        return ResponseEntity.ok(ragService.listDocuments());
    }
}
