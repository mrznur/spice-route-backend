package com.spiceroute.feedback.ai;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the prompt sent to Gemini.
 * Comments are PII-stripped and numbered.
 * The model is instructed to return structured JSON only.
 */
final class AiPromptBuilder {

    private AiPromptBuilder() {}

    static String build(List<String> strippedComments) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are analysing customer feedback for a restaurant.
                Your task is to identify patterns across the comments below.

                Return ONLY valid JSON — no markdown, no explanation, no extra text.
                Use this exact schema:
                {
                  "overallSentiment": "positive" | "mixed" | "negative",
                  "praises": [
                    { "finding": "string", "mentionCount": integer, "examples": ["string", ...] }
                  ],
                  "complaints": [
                    { "finding": "string", "mentionCount": integer, "examples": ["string", ...] }
                  ],
                  "recurringIssues": ["string", ...],
                  "suggestions": ["string", ...]
                }

                Rules:
                - Only report findings that are actually present in the comments.
                - mentionCount must be a realistic count of how many comments mention that finding.
                - examples must be short direct quotes taken verbatim from the comments below.
                - Do not invent, assume, or extrapolate anything not in the comments.
                - If a category has no findings, return an empty array for that field.
                - Respond in the same language as the majority of the comments.

                """);

        sb.append("COMMENTS (").append(strippedComments.size()).append(" total):\n");
        AtomicInteger n = new AtomicInteger(1);
        strippedComments.forEach(c ->
                sb.append(n.getAndIncrement()).append(". ").append(c).append("\n"));

        return sb.toString();
    }
}
