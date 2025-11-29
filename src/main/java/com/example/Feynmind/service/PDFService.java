package com.example.Feynmind.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class PdfService {

    public String extractTextFromPdf(MultipartFile file) {
        // Try-with-resources ensures the document closes automatically to prevent memory leaks
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            
            // Create the stripper (this pulls the text out)
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Extract and return the text
            return stripper.getText(document);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }
}