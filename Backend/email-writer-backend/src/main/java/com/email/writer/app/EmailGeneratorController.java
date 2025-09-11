package com.email.writer.app;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateEmail(@RequestBody EmailRequest emailRequest) {
        try {
            // Debug logging
            System.out.println("=== EMAIL GENERATION REQUEST ===");
            System.out.println("Subject: " + emailRequest.getSubject());
            System.out.println("Email Content: " + emailRequest.getEmailContent());
            System.out.println("Tone: " + emailRequest.getTone());

            // Call service
            String aiResponse = emailGeneratorService.generateEmailReply(emailRequest);

            System.out.println("=== AI RESPONSE ===");
            System.out.println(aiResponse);

            // Improved parsing: split summary vs reply
            String summary = "";
            String reply = aiResponse;

            // Try multiple parsing strategies
            if (aiResponse.toLowerCase().contains("summary:")) {
                // Strategy 1: Look for "Reply:" marker
                int replyIndex = aiResponse.toLowerCase().indexOf("reply:");
                if (replyIndex == -1) {
                    // Strategy 2: Look for numbered sections like "2)" or "2."
                    replyIndex = findReplySection(aiResponse);
                }

                if (replyIndex != -1) {
                    // Extract summary part
                    summary = aiResponse.substring(0, replyIndex)
                            .replaceAll("(?i)summary:?", "")
                            .replaceAll("1\\)", "")
                            .replaceAll("1\\.", "")
                            .trim();

                    // Extract reply part
                    reply = aiResponse.substring(replyIndex)
                            .replaceAll("(?i)reply:?", "")
                            .replaceAll("2\\)", "")
                            .replaceAll("2\\.", "")
                            .trim();
                }
            } else if (aiResponse.contains("\n\n")) {
                // Strategy 3: Split by double newlines if no clear markers
                String[] parts = aiResponse.split("\n\n", 2);
                if (parts.length == 2) {
                    summary = parts[0].trim();
                    reply = parts[1].trim();
                }
            }

            // Clean up the reply to remove any subject lines or headers
            reply = cleanReply(reply);

            System.out.println("=== PARSED RESULT ===");
            System.out.println("Summary: " + summary);
            System.out.println("Reply: " + reply);

            // Return structured JSON
            return ResponseEntity.ok(Map.of(
                    "summary", summary,
                    "reply", reply
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to generate email",
                    "details", e.getMessage()
            ));
        }
    }

    // Test endpoint to verify Gemini API connection
    @GetMapping("/test")
    public ResponseEntity<?> testGeminiConnection() {
        try {
            // Simple test request
            EmailRequest testRequest = new EmailRequest();
            testRequest.setSubject("Test");
            testRequest.setEmailContent("This is a test email");
            testRequest.setTone("professional");

            String response = emailGeneratorService.generateEmailReply(testRequest);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Gemini API is working",
                    "response", response
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Gemini API test failed",
                    "error", e.getMessage()
            ));
        }
    }

    private int findReplySection(String text) {
        String lowerText = text.toLowerCase();

        // Look for common reply indicators
        String[] replyMarkers = {"2)", "2.", "reply:", "response:", "then,", "second,"};

        for (String marker : replyMarkers) {
            int index = lowerText.indexOf(marker);
            if (index != -1) {
                return index;
            }
        }

        return -1;
    }

    private String cleanReply(String reply) {
        // Remove any subject line that might be included
        reply = reply.replaceAll("(?i)subject:\\s*re:.*?\n", "");
        reply = reply.replaceAll("(?i)subject:.*?\n", "");

        // Remove common email headers
        reply = reply.replaceAll("(?i)to:.*?\n", "");
        reply = reply.replaceAll("(?i)from:.*?\n", "");

        // Clean up extra whitespace
        reply = reply.trim();

        return reply;
    }
}