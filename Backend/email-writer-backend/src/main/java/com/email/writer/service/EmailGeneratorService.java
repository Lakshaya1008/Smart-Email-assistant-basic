package com.email.writer.service;

import com.email.writer.dto.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.*;

/**
 * Service that calls the Gemini API (via WebClient) to generate replies.
 *
 * Design:
 * - Uses WebClient for non-blocking HTTP; but blocks for simplicity (this can be made reactive).
 * - Builds structured prompts and parses responses. Keeps backward compatibility for single-reply endpoint.
 *
 * Multi-language:
 * - Accepts language parameter on EmailRequest and includes it in prompt instructions.
 *
 * Regenerate:
 * - If regenerate=true, we tweak the prompt (add seed/timestamp) to encourage different outputs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailGeneratorService {

    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl = "";

    @Value("${gemini.api.endpoint}")
    private String geminiApiEndpoint = "";

    @Value("${gemini.api.key}")
    private String geminiApiKey = "";

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(geminiApiUrl).build();
    }

    /**
     * Generate three reply variations plus a short summary.
     *
     * @param request    incoming email details
     * @param regenerate if true, modify prompt slightly to get more varied responses
     * @return map with "summary" and "replies" (list of 3 strings)
     */
    public Map<String, Object> generateMultipleEmailReplies(EmailRequest request, boolean regenerate) {
        final String language = (request.getLanguage() == null || request.getLanguage().isBlank()) ? "en" : request.getLanguage();
        String prompt = buildMultipleRepliesPrompt(request, language, regenerate);
        log.debug("Built multiple replies prompt: {}", prompt);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", regenerate ? 0.9 : 0.75,
                        "maxOutputTokens", 2048,
                        "topP", 0.95,
                        "topK", 40
                )
        );

        try {
            String uri = geminiApiEndpoint + "?key=" + geminiApiKey;
            String raw = webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Raw Gemini response: {}", raw == null ? "null" : raw.substring(0, Math.min(1000, raw.length())));
            return parseMultipleRepliesResponse(raw);
        } catch (Exception ex) {
            log.error("Gemini API call failed", ex);
            throw new RuntimeException("Failed to call Gemini API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Generates a single email reply with summary.
     * @param request The email request containing subject, content, tone, and language
     * @return A map containing both the summary and the reply
     */
    public Map<String, String> generateEmailReply(EmailRequest request) {
        final String language = (request.getLanguage() == null || request.getLanguage().isBlank()) ? "en" : request.getLanguage();
        String prompt = buildSingleReplyPrompt(request, language);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 1024,
                        "topP", 0.8,
                        "topK", 40
                )
        );

        try {
            String uri = geminiApiEndpoint + "?key=" + geminiApiKey;
            String raw = webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String response = extractResponseContent(raw);
            
            // Parse the response to extract summary and reply
            String summary = "";
            String reply = "";
            
            if (response != null && !response.isBlank()) {
                // Split the response into summary and reply parts
                String[] parts = response.split("(?i)Reply:");
                if (parts.length > 0) {
                    // Extract summary (remove "Summary:" prefix if present)
                    String summaryPart = parts[0].replaceFirst("(?i)Summary:\s*", "").trim();
                    summary = summaryPart;
                    
                    // If there's a reply part, extract it
                    if (parts.length > 1) {
                        reply = parts[1].trim();
                    }
                }
            }
            
            // If we couldn't parse the response, use fallback values
            if (summary.isEmpty()) {
                summary = "Summary not available";
            }
            if (reply.isEmpty()) {
                reply = response; // Fallback to the full response if we couldn't parse it
            }
            
            return Map.of(
                "summary", summary,
                "reply", reply
            );
            
        } catch (Exception ex) {
            log.error("Error calling Gemini for single reply", ex);
            throw new RuntimeException("Failed to call Gemini API: " + ex.getMessage(), ex);
        }
    }

    /* ---------------------- Prompt builders ---------------------- */

    private String buildMultipleRepliesPrompt(EmailRequest request, String language, boolean regenerate) {
        StringBuilder prompt = new StringBuilder();

        // Base instructions
        prompt.append("You are an expert email assistant. Generate three different professional email replies.\n");
        prompt.append("IMPORTANT: Generate the reply in ").append(language).append(" language.\n\n");

        // Add tone instruction
        if (request.getTone() != null && !request.getTone().isBlank()) {
            prompt.append("Use a ").append(request.getTone()).append(" tone.\n\n");
        }

        // Add regeneration marker if needed
        if (regenerate) {
            prompt.append("IMPORTANT: Generate completely new variations different from previous ones. Timestamp: ")
                  .append(Instant.now().toString())
                  .append("\n\n");
        }

        // Email context
        prompt.append("Original Email Subject: ").append(request.getSubject()).append("\n");
        prompt.append("Original Email Content:\n").append(request.getEmailContent()).append("\n\n");

        // Output format instructions
        prompt.append("Please provide:\n");
        prompt.append("1. Three different reply variations (marked as REPLY 1:, REPLY 2:, REPLY 3:)\n");
        prompt.append("2. A brief summary of the key points addressed (marked as SUMMARY:)\n");

        return prompt.toString();
    }

    private String buildSingleReplyPrompt(EmailRequest request, String language) {
        return String.format(
            "You are a professional email assistant. Provide a short Summary and a single Reply body in %s.\n" +
            "Summary: (1-2 sentences)\n" +
            "Reply: (3-5 sentences, body only, no salutation/signature)\n\n" +
            "EMAIL:\nSubject: %s\n" +
            "Content: %s\n" +
            "Tone: %s\n",
            language,
            request.getSubject() == null ? "" : request.getSubject(),
            request.getEmailContent() == null ? "" : request.getEmailContent(),
            request.getTone() == null ? "professional" : request.getTone()
        );
    }

    /* ---------------------- Response parsing ---------------------- */

    private Map<String, Object> parseMultipleRepliesResponse(String raw) {
        try {
            String content = extractResponseContent(raw);
            String summary = "";
            List<String> replies = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            String mode = "none";

            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.toLowerCase().startsWith("summary:")) {
                    if (current.length() > 0 && mode.startsWith("reply")) {
                        replies.add(current.toString().trim());
                        current.setLength(0);
                    }
                    summary = line.replaceFirst("(?i)summary:\\s*", "").trim();
                    mode = "summary";
                } else if (line.toLowerCase().matches("reply\\s*[123]:.*")) {
                    if (current.length() > 0 && mode.startsWith("reply")) {
                        replies.add(current.toString().trim());
                    }
                    current.setLength(0);
                    mode = "reply";
                    current.append(line.replaceFirst("(?i)reply\\s*[123]:\\s*", "").trim());
                } else if (mode.startsWith("reply")) {
                    if (current.length() > 0) current.append(" ");
                    current.append(line);
                }
            }

            if (current.length() > 0 && mode.startsWith("reply")) {
                replies.add(current.toString().trim());
            }

            // Ensure exactly 3 replies
            while (replies.size() < 3) {
                replies.add("Thank you for your message. I will review and respond shortly.");
            }
            if (replies.size() > 3) {
                replies = replies.subList(0, 3);
            }

            return Map.of(
                "replies", replies,
                "summary", summary.isEmpty() ? "Email received and understood." : summary
            );
        } catch (Exception ex) {
            log.error("Failed to parse multiple replies", ex);
            return Map.of(
                "summary", "Unable to parse summary",
                "replies", List.of(
                    "Thank you for your message. I will review and respond shortly.",
                    "I appreciate you reaching out. I will consider and respond soon.",
                    "I've received your email and will follow up soon."
                )
            );
        }
    }

    /**
     * Extracts candidate text from the Gemini-like raw JSON.
     * Adjust this method to match the exact response structure from your LLM provider.
     */
    private String extractResponseContent(String raw) {
        if (raw == null || raw.isBlank()) return "";

        try {
            JsonNode root = mapper.readTree(raw);
            
            // Check for Gemini 1.0 format
            if (root.has("candidates")) {
                JsonNode candidate = root.path("candidates").get(0);
                if (!candidate.isMissingNode()) {
                    JsonNode content = candidate.path("content");
                    if (!content.isMissingNode()) {
                        JsonNode parts = content.path("parts");
                        if (parts.isArray() && parts.size() > 0) {
                            JsonNode textNode = parts.get(0).path("text");
                            if (!textNode.isMissingNode()) {
                                return textNode.asText().trim();
                            }
                        }
                    }
                }
            }
            
            // Check for Gemini 1.5 format
            if (root.has("candidates")) {
                JsonNode candidate = root.path("candidates").get(0);
                if (!candidate.isMissingNode()) {
                    JsonNode textNode = candidate.path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        return textNode.asText().trim();
                    }
                }
            }
            
            // Fallback to direct text extraction
            return root.has("output") ? root.path("output").asText().trim() : raw;
            
        } catch (Exception ex) {
            log.warn("Unable to parse JSON response, returning raw string", ex);
            log.debug("Raw response that failed to parse: {}", raw);
            return raw;
        }
    }
}
