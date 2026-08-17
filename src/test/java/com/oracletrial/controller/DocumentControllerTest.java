package com.oracletrial.controller;

import com.oracletrial.service.DocumentService;
import com.oracletrial.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Plain unit test for the upload endpoint's input validation. The
 * controller is built directly with mocked services, so this doesn't need
 * a Spring web context - it just checks that bad input is rejected before
 * any PDF processing is attempted.
 */
class DocumentControllerTest {

    private final DocumentController controller =
            new DocumentController(mock(DocumentService.class), mock(EmbeddingService.class));

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> controller.uploadPdf(emptyFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPdfFile() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "just some text".getBytes());

        assertThatThrownBy(() -> controller.uploadPdf(textFile))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
