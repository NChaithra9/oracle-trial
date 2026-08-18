package com.oracle.trial.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

    /**
     * Renders a single page as a PNG image. Used as a fallback for pages
     * that PDFBox couldn't pull normal text out of (scanned pages, photos,
     * charts, diagrams) so the image can be sent to a vision-capable model
     * for description instead.
     *
     * @param pdfBytes   the raw bytes of the uploaded PDF
     * @param pageNumber 1-based page number to render
     * @return the rendered page as PNG image bytes
     */
    public byte[] renderPageAsPng(byte[] pdfBytes, int pageNumber) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            // 150 DPI is plenty of detail for a vision model to read text/
            // charts while keeping the image (and the API request) small.
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 150);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
