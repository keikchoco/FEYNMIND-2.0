package com.example.Feynmind.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class PdfService {

    public String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            if (text == null || text.trim().isEmpty()) {
                return "Error: No text found. This PDF might be an image scan.";
            }
            return text.trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error parsing PDF: " + e.getMessage();
        }
    }
}