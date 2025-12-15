package com.example.Feynmind.controller;

import com.example.Feynmind.model.StudyMaterial;
import com.example.Feynmind.repository.StudyMaterialRepository;
import com.example.Feynmind.service.AiService;
import com.example.Feynmind.service.PdfService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    private final PdfService pdfService;
    private final AiService aiService;
    private final StudyMaterialRepository repository;

    public DocumentController(PdfService pdfService, AiService aiService, StudyMaterialRepository repository) {
        this.pdfService = pdfService;
        this.aiService = aiService;
        this.repository = repository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        System.out.println("------------------------------------------------");
        System.out.println("📂 NEW UPLOAD REQUEST: " + file.getOriginalFilename());

        try {
            // 1. EXTRACT
            System.out.println("📄 Step 1: Extracting text from PDF...");
            String extractedText = pdfService.extractTextFromPdf(file);
            System.out.println("✅ Text extracted. Length: " + extractedText.length() + " chars");

            // 2. CHECK DATABASE (Handle Duplicates Safely)
            System.out.println("💾 Step 2: Checking database for existing file...");
            List<StudyMaterial> existingFiles = repository.findByFileName(file.getOriginalFilename());
            
            StudyMaterial material;
            if (!existingFiles.isEmpty()) {
                System.out.println(" Found " + existingFiles.size() + " existing copies. Updating the first one.");
                material = existingFiles.get(0);
                material.setContent(extractedText);
            } else {
                System.out.println("No duplicates found. Creating new entry.");
                material = new StudyMaterial(file.getOriginalFilename(), extractedText);
            }
            
            repository.save(material);
            System.out.println("✅ Saved to database successfully.");

            // 3. ANALYZE
            System.out.println(" Step 3: Sending to AI for analysis...");
            String summary = aiService.generateStudyTopics(extractedText);
            System.out.println("✅ AI Response received: " + summary);

            // 4. RESPONSE
            Map<String, String> response = new HashMap<>();
            response.put("fileName", file.getOriginalFilename());
            response.put("topics", summary);
            
            System.out.println(" Step 4: Sending response to frontend.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" CRITICAL ERROR IN UPLOAD:");
            e.printStackTrace(); 
            return ResponseEntity.internalServerError().body("Backend Error: " + e.getMessage());
        }
    }
}