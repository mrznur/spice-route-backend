package com.spiceroute.feedback.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spiceroute.feedback.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin REST client for the Gemini generateContent API.
 * Uses Java 21 HttpClient — no extra dependencies required.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiClient(
            @Value("${app.gemini.api-key}") String apiKey,
            @Value("${app.gemini.model:gemini-flash-lite-latest}") String model,
            @Value("${app.gemini.timeout-seconds:20}") int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model  = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        if ("disabled".equals(apiKey)) {
            log.warn("GEMINI_API_KEY is not set — AI summary feature will return 503 until configured.");
        }
    }

    public record GeminiResult(String text, int inputTokens, int outputTokens) {}

    /**
     * Sends a prompt to Gemini and returns the text response plus token counts.
     * Throws ApiException on timeout or non-2xx response.
     */
    public GeminiResult generate(String prompt) {
        if ("disabled".equals(apiKey)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI features are not configured. Set the GEMINI_API_KEY environment variable.");
        }
        String url = BASE + model + ":generateContent?key=" + apiKey;

        // Escape the prompt for JSON — ObjectMapper handles all edge cases
        String escapedPrompt;
        try {
            escapedPrompt = mapper.writeValueAsString(prompt);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build AI request");
        }

        String body = """
                {
                  "contents": [{"parts": [{"text": %s}]}],
                  "generationConfig": {
                    "responseMimeType": "application/json",
                    "temperature": 0.2,
                    "maxOutputTokens": 2048
                  }
                }
                """.formatted(escapedPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(timeout)
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException ex) {
            log.warn("Gemini request timed out after {}s", timeout.getSeconds());
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                    "AI service timed out. Please try again in a moment.");
        } catch (IOException | InterruptedException ex) {
            log.error("Gemini request failed", ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI service unavailable");
        }

        if (response.statusCode() != 200) {
            log.warn("Gemini returned HTTP {}: {}", response.statusCode(),
                    response.body().substring(0, Math.min(300, response.body().length())));
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "AI service returned an error. Please try again.");
        }

        try {
            JsonNode root = mapper.readTree(response.body());

            // Extract text
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // Extract token counts (best-effort — Gemini may omit these)
            JsonNode usage = root.path("usageMetadata");
            int inputTokens  = usage.path("promptTokenCount").asInt(0);
            int outputTokens = usage.path("candidatesTokenCount").asInt(0);

            return new GeminiResult(text, inputTokens, outputTokens);
        } catch (Exception ex) {
            log.error("Failed to parse Gemini response: {}", response.body(), ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not parse AI response");
        }
    }
}
