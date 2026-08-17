package com.oracletrial.service;

import com.oracletrial.model.DocumentChunk;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the PDF-to-chunks pipeline in {@link DocumentService}: extraction,
 * cleaning, and chunking. A tiny PDF is built in memory with PDFBox itself,
 * so these tests don't depend on any external file or network call.
 */
class DocumentServiceTest {

    private final DocumentService documentService = new DocumentService();

    @Test
    void extractTextReadsBackTheTextThatWasWrittenIntoThePdf() throws IOException {
        MockMultipartFile pdf = pdfContaining("Employees receive 24 days of annual leave.");

        String extracted = documentService.extractText(pdf);

        assertThat(extracted).contains("Employees receive 24 days of annual leave.");
    }

    @Test
    void cleanTextCollapsesRepeatedWhitespaceAndBlankLines() {
        String messy = "Hello    world\n\n\n\nGoodbye\t\tworld";

        String cleaned = documentService.cleanText(messy);

        assertThat(cleaned).isEqualTo("Hello world\n\nGoodbye world");
    }

    @Test
    void cleanTextReturnsEmptyStringForNullInput() {
        assertThat(documentService.cleanText(null)).isEmpty();
    }

    @Test
    void chunkTextSplitsLongTextIntoNumberedOverlappingChunks() {
        String longText = String.join(" ", java.util.Collections.nCopies(1000, "word"));

        List<DocumentChunk> chunks = documentService.chunkText("hr-policy.pdf", longText);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).getChunk()).isEqualTo(1);
        assertThat(chunks.get(0).getDocument()).isEqualTo("hr-policy.pdf");
        // Consecutive chunks share an overlap, so the tail of one chunk
        // should reappear near the start of the next.
        String[] firstChunkWords = chunks.get(0).getText().split(" ");
        assertThat(firstChunkWords.length).isLessThanOrEqualTo(600);
    }

    @Test
    void chunkTextReturnsEmptyListForBlankText() {
        assertThat(documentService.chunkText("doc.pdf", "   ")).isEmpty();
    }

    /** Builds a one-page PDF containing the given text, wrapped as an upload. */
    private MockMultipartFile pdfContaining(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);

            return new MockMultipartFile("file", "test.pdf", "application/pdf", out.toByteArray());
        }
    }
}
