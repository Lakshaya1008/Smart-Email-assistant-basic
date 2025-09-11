package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        // Build the improved prompt
        String prompt = buildPrompt(emailRequest);

        System.out.println("=== BUILT PROMPT ===");
        System.out.println(prompt);
        System.out.println("=== API URL ===");
        System.out.println(geminiApiUrl + geminiApiKey.substring(0, 10) + "...");

        // Craft request for Gemini 2.5 Flash
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                },
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 1024,
                        "topP", 0.8,
                        "topK", 40
                )
        );

        try {
            // Send request
            String response = webClient.post()
                    .uri(geminiApiUrl + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("=== RAW GEMINI RESPONSE ===");
            System.out.println(response);

            // Extract response
            return extractResponseContent(response);

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Check for error in response
            if (rootNode.has("error")) {
                String errorMessage = rootNode.path("error").path("message").asText();
                System.err.println("Gemini API Error: " + errorMessage);
                return "API Error: " + errorMessage;
            }

            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText()
                    .trim();

        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
            return "Error processing response: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a professional email assistant. Analyze the following email and provide a structured response.\n\n");

        prompt.append("Instructions:\n");
        prompt.append("1. Write a brief summary (1-2 sentences) of what the email is about\n");
        prompt.append("2. Write a professional email reply body ONLY (3-5 sentences)\n");
        prompt.append("3. Do NOT include subject line, greeting, or signature in the reply\n");
        prompt.append("4. Just provide the main message content\n\n");

        prompt.append("Format your response EXACTLY like this:\n");
        prompt.append("Summary: [Write your summary here]\n\n");
        prompt.append("Reply: [Write only the main message content here]\n\n");

        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("IMPORTANT: Use a ").append(emailRequest.getTone()).append(" tone in your reply.\n\n");
        }

        prompt.append("EMAIL TO ANALYZE:\n");
        prompt.append("Subject: ").append(emailRequest.getSubject()).append("\n");
        prompt.append("Content: ").append(emailRequest.getEmailContent()).append("\n\n");

        prompt.append("Provide the summary and reply now (remember: no subject line in reply):");

        return prompt.toString();
    }
}
