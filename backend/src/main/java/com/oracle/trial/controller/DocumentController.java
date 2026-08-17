package com.oracle.trial.controller;

import com.oracle.trial.model.UploadResponse;
import com.oracle.trial.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Handles PDF uploads. The frontend supports uploading 5+ PDFs simply by
 * calling this endpoint once per file; every document is added to the same
 * Qdrant collection, tagged with its own document name and page numbers.
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
                    .body(new UploadResponse("File is empty", file.getOriginalFilename(), 0, 0));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("Only PDF files are supported", filename, 0, 0));
        }

        UploadResponse response = ragService.processUpload(file);
        return ResponseEntity.ok(response);
    }
}
