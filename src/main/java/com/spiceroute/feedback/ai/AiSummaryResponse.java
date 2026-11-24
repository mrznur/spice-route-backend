package com.spiceroute.feedback.ai;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Returned to the frontend after a successful AI analysis.
 * All findings come directly from the AI response — never calculated frontend-side.
 */
public record AiSummaryResponse(
        UUID summaryId,
        UUID branchId,
        LocalDate dateFrom,
        LocalDate dateTo,
        int commentCount,
        boolean cached,
        Instant generatedAt,

        // AI-generated fields
        String overallSentiment,          // "positive" | "mixed" | "negative"
        List<Finding> praises,
        List<Finding> complaints,
        List<String> recurringIssues,
        List<String> suggestions
) {
    public record Finding(
            String finding,
            int mentionCount,
            List<String> examples           // actual quotes from the comments, PII already stripped
    ) {}
}
