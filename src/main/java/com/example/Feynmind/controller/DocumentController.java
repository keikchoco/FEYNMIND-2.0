package com.example.Feynmind.controller;

import com.example.Feynmind.model.StudyMaterial;
import com.example.Feynmind.repository.StudyMaterialRepository;
import com.example.Feynmind.service.PdfService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*") 
public class DocumentController {

    private final PdfService pdfService;
    private final StudyMaterialRepository repository;

    public DocumentController(PdfService pdfService, StudyMaterialRepository repository) {
        this.pdfService = pdfService;
        this.repository = repository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
        // 1. Extract text using our Service
        String extractedText = pdfService.extractTextFromPdf(file);

        // 2. Save to Database
        StudyMaterial material = new StudyMaterial(file.getOriginalFilename(), extractedText);
        repository.save(material);

        return ResponseEntity.ok("PDF processed successfully! extracted " + extractedText.length() + " characters.");
    }
}