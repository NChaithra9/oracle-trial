package com.oracle.trial.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Extracts text from an uploaded PDF, page by page, using Apache PDFBox.
 * Keeping text separated by page is what lets us later cite an exact page
 * number for every answer.
 */
@Service
public class PdfService {

    /**
     * @param pdfBytes the raw bytes of the uploaded PDF
     * @return a map of pageNumber (1-based) -> extracted text for that page
     */
    public Map<Integer, String> extractTextByPage(byte[] pdfBytes) throws IOException {
        Map<Integer, String> pageTextMap = new LinkedHashMap<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                pageTextMap.put(page, text == null ? "" : text.trim());
            }
        }

        return pageTextMap;
    }
}
