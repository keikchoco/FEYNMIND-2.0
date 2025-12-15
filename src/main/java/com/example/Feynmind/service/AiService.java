package com.example.Feynmind.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    // --- GEMINI CONFIG ONLY ---
    @Value("${gemini.api.key}")
    private String geminiKey;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    // --- MAIN METHOD: CALL GEMINI ---
    private String callGemini(String promptText) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", promptText)))
            )
        );

        try {
            URI uri = URI.create(geminiUrl + "?key=" + geminiKey);
            System.out.println(" Sending request to Gemini...");
            
            String response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
                
            return extractTextFromGemini(response);

        } catch (Exception e) {
            System.err.println(" GEMINI ERROR: " + e.getMessage());
            // Return error as a JSON list so the frontend doesn't crash
            String cleanError = e.getMessage().replace("\"", "'");
            return "[\"Error calling Gemini: " + cleanError + "\"]";
        }
    }

    private String extractTextFromGemini(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            // CLEANUP: Remove markdown formatting if Gemini adds it (e.g. ```json ... ```)
            return text.replaceAll("```json", "").replaceAll("```", "").trim();
            
        } catch (Exception e) {
            return "[\"Error parsing Gemini JSON: " + e.getMessage() + "\"]";
        }
    }

    // --- FEATURES (ALL USING GEMINI NOW) ---

    public String generateStudyTopics(String pdfText) {
        String cleanText = pdfText.length() > 5000 ? pdfText.substring(0, 5000) : pdfText;
        // Instruction to ensure valid JSON output
        String prompt = "You are a study assistant. Extract the 5 most important concepts from the text below. Return them strictly as a JSON list of strings (e.g. [\"Concept 1\", \"Concept 2\"]). Do not add markdown formatting. Text: " + cleanText;
        return callGemini(prompt);
    }

    // UPDATED: Now accepts 'difficulty'
    public String assessStudentExplanation(String concept, String explanation, String difficulty) {
        
        String toneInstruction = "";

        // Customize the persona based on difficulty
        switch (difficulty.toLowerCase()) {
            case "easy":
                toneInstruction = "You are a gentle, encouraging tutor teaching a beginner. Use simple language (ELIF5), avoid jargon, and focus on the big picture. If they are close, give them credit.";
                break;
            case "hard":
                toneInstruction = "You are a strict, Socratic professor at a top university. Challenge the user's assumptions, demand precise terminology, and point out even small logical flaws.";
                break;
            case "medium":
            default:
                toneInstruction = "You are a helpful study assistant. Verify accuracy and correct mistakes clearly, but keep the conversation flowing naturally.";
                break;
        }

        String prompt = toneInstruction + "\n\n" +
                        "Concept: " + concept + "\n" +
                        "Student Explanation: " + explanation + "\n\n" +
                        "Provide feedback on their explanation.";

        return callGemini(prompt);
    }

    // UPDATED: Now accepts 'difficulty'
    public String generateAnalogy(String concept, String difficulty) {
        
        String styleInstruction = "";

        switch (difficulty.toLowerCase()) {
            case "easy":
                styleInstruction = "Use a very simple, real-world analogy (like cooking, sports, or simple machines) that a 10-year-old could understand.";
                break;
            case "hard":
                styleInstruction = "Use a sophisticated, abstract, or technical analogy suitable for an expert or graduate student.";
                break;
            case "medium":
            default:
                styleInstruction = "Use a standard, relatable analogy suitable for a college student.";
                break;
        }

        String prompt = "Give a creative analogy to explain the concept: " + concept + ".\n" +
                        styleInstruction + "\n" +
                        "Keep it concise.";

        return callGemini(prompt);
    }
}