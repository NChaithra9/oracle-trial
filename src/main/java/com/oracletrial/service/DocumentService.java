package com.oracletrial.service;

import com.oracletrial.model.DocumentChunk;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Turns an uploaded PDF into clean, embeddable chunks of text.
 *
 * <p>This is the first stage of the RAG pipeline: extract raw text from the
 * PDF, clean it up, then split it into small chunks. Embedding and Qdrant
 * storage are added in a later milestone.</p>
 */
@Service
public class DocumentService {

    /** Number of words in each chunk. Kept small and easy to change later. */
    private static final int CHUNK_SIZE = 600;

    /** Number of words repeated between two consecutive chunks. */
    private static final int CHUNK_OVERLAP = 80;

    /**
     * Extracts readable text from an uploaded PDF.
     *
     * @param file uploaded PDF
     * @return raw extracted text
     * @throws IOException if the PDF cannot be read
     */
    public String extractText(MultipartFile file) throws IOException {

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper pdfTextStripper = new PDFTextStripper();

            return pdfTextStripper.getText(document);
        }
    }

    /**
     * Cleans raw PDF text before it is chunked and embedded.
     *
     * <p>PDF extraction often leaves behind messy formatting: repeated
     * spaces, stray tabs, and long runs of blank lines. Cleaning this up
     * keeps chunks readable and stops the embedding model from wasting its
     * limited context on formatting noise instead of actual content.</p>
     *
     * @param rawText text straight out of {@link #extractText(MultipartFile)}
     * @return cleaned text with normalized whitespace and blank lines
     */
    public String cleanText(String rawText) {
        if (rawText == null) {
            return "";
        }

        // Collapse repeated spaces/tabs into a single space.
        String withoutExtraSpaces = rawText.replaceAll("[ \t]{2,}", " ");

        // Collapse 3+ blank lines into a single blank line between paragraphs.
        String withoutExtraBlankLines = withoutExtraSpaces.replaceAll("\n{3,}", "\n\n");

        return withoutExtraBlankLines.trim();
    }

    /**
     * Splits cleaned document text into overlapping chunks.
     *
     * <p>A whole document is too large and covers too many topics to embed
     * as a single vector, so it would never match a specific question well.
     * Splitting it into smaller chunks lets the search step find just the
     * few chunks that actually answer a question. A small overlap between
     * chunks avoids cutting an important sentence in half at a chunk
     * boundary.</p>
     *
     * @param documentName name of the source document, used for citations later
     * @param cleanedText  text returned by {@link #cleanText(String)}
     * @return ordered list of chunks, numbered starting at 1
     */
    public List<DocumentChunk> chunkText(String documentName, String cleanedText) {
        List<DocumentChunk> chunks = new ArrayList<>();

        if (cleanedText == null || cleanedText.isBlank()) {
            return chunks;
        }

        String[] words = cleanedText.split("\\s+");
        int chunkNumber = 1;
        int start = 0;

        while (start < words.length) {
            int end = Math.min(start + CHUNK_SIZE, words.length);
            String chunkText = String.join(" ", Arrays.copyOfRange(words, start, end));

            // Page number is not tracked yet; left null until page-aware extraction is added.
            chunks.add(new DocumentChunk(chunkText, documentName, chunkNumber, null));
            chunkNumber++;

            if (end == words.length) {
                break;
            }

            // Step back by the overlap so the next chunk repeats a bit of this one.
            start = end - CHUNK_OVERLAP;
        }

        return chunks;
    }
}