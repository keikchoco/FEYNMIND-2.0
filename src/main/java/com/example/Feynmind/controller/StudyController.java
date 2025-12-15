package com.example.Feynmind.controller;

import com.example.Feynmind.model.StudyMaterial;
import com.example.Feynmind.repository.StudyMaterialRepository;
import com.example.Feynmind.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study")
@CrossOrigin(origins = "http://localhost:5173") // Ensure this matches your frontend port
public class StudyController {

    private final AiService aiService;
    private final StudyMaterialRepository repository;

    public StudyController(AiService aiService, StudyMaterialRepository repository) {
        this.aiService = aiService;
        this.repository = repository;
    }

    // --- HELPER METHOD ---
    private String getTextFromDb(String fileName) {
        List<StudyMaterial> materials = repository.findByFileName(fileName);
        
        if (materials.isEmpty()) {
            return null;
        }
        // Safely get the first file found
        return materials.get(0).getContent();
    }

    // --- ENDPOINTS ---

    @PostMapping("/analyze")
    public String getTopics(@RequestBody Map<String, String> payload) {
        String text = getTextFromDb(payload.get("fileName"));
        if (text == null) return "Error: File not found in database.";
        return aiService.generateStudyTopics(text);
    }

    @PostMapping("/feynman-check")
    public String checkUnderstanding(@RequestBody Map<String, String> payload) {
        // 1. Extract Difficulty (Default to 'medium' if null)
        String difficulty = payload.getOrDefault("difficulty", "medium");

        // 2. Pass concept, explanation, AND difficulty to the Service
        return aiService.assessStudentExplanation(
            payload.get("concept"), 
            payload.get("explanation"),
            difficulty
        );
    }

    @PostMapping("/analogy")
    public String getAnalogy(@RequestBody Map<String, String> payload) {
        // 1. Extract Difficulty
        String difficulty = payload.getOrDefault("difficulty", "medium");

        // 2. Pass concept AND difficulty to the Service
        return aiService.generateAnalogy(
            payload.get("concept"),
            difficulty
        );
    }

    @PostMapping("/log-progress")
    public String logProgress(@RequestBody Map<String, Object> payload) {
        // Useful for debugging if data is reaching the backend
        System.out.println("=== LOG: " + payload.get("concept") + " - Difficulty: " + payload.get("difficulty"));
        return "Logged.";
    }
}