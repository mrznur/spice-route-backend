
package com.spiceroute.feedback.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spiceroute.feedback.branch.BranchService;
import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.AiSummary;
import com.spiceroute.feedback.domain.AiUsageLog;
import com.spiceroute.feedback.domain.Feedback;
import com.spiceroute.feedback.repository.AiSummaryRepository;
import com.spiceroute.feedback.repository.AiUsageLogRepository;
import com.spiceroute.feedback.repository.FeedbackSpecs;
import com.spiceroute.feedback.repository.FeedbackRepository;
import com.spiceroute.feedback.security.CurrentUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AiSummaryService {

    private static final Logger log =
            LoggerFactory.getLogger(AiSummaryService.class);

    private static final int MIN_COMMENTS = 5;
    private static final int MAX_RANGE_DAYS = 365;

    private final FeedbackRepository feedbacks;
    private final AiSummaryRepository summaries;
    private final AiUsageLogRepository usageLogs;
    private final AiUsageLogService usageLogService;
    private final BranchService branchService;
    private final GeminiClient gemini;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiSummaryService(
            FeedbackRepository feedbacks,
            AiSummaryRepository summaries,
            AiUsageLogRepository usageLogs,
            AiUsageLogService usageLogService,
            BranchService branchService,
            GeminiClient gemini
    ) {
        this.feedbacks = feedbacks;
        this.summaries = summaries;
        this.usageLogs = usageLogs;
        this.usageLogService = usageLogService;
        this.branchService = branchService;
        this.gemini = gemini;
    }

    @Transactional
    public AiSummaryResponse summarise(AiSummaryRequest req) {
        UUID restaurantId = CurrentUser.restaurantId();
        UUID userId = CurrentUser.get().userId();

        // Enforce branch access, including manager scope.
        branchService.requireAccessible(req.branchId());

        // Validate date range.
        if (req.dateFrom().isAfter(req.dateTo())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dateFrom must not be after dateTo"
            );
        }

        if (req.dateFrom().until(
                req.dateTo(),
                java.time.temporal.ChronoUnit.DAYS
        ) > MAX_RANGE_DAYS) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Date range cannot exceed 1 year"
            );
        }

        // Fetch comments for the authorized branch and date range.
        Instant from = req.dateFrom()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant to = req.dateTo()
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        var spec = FeedbackSpecs.search(
                restaurantId,
                Set.of(req.branchId()),
                req.branchId(),
                null,
                null,
                from,
                to
        );

        List<Feedback> all = feedbacks.findAll(spec);

        // Keep only rows that have a meaningful comment.
        List<String> rawComments = all.stream()
                .map(Feedback::getComment)
                .filter(c -> c != null && !c.isBlank() && c.trim().length() > 3)
                .toList();

        if (rawComments.size() < MIN_COMMENTS) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Not enough feedback to generate a summary. "
                            + "At least " + MIN_COMMENTS + " comments are required "
                            + "(found " + rawComments.size() + ")."
            );
        }

        // Strip PII from every comment before hashing or sending to AI.
        List<String> stripped = rawComments.stream()
                .map(PiiStripper::strip)
                .filter(s -> !s.isBlank())
                .toList();

        // Compute cache key: SHA-256 of sorted stripped comments.
        String inputHash = hash(stripped);

        // Cache hit — return stored result instantly, no AI call.
        var cached = summaries.findByRestaurantIdAndBranchIdAndDateFromAndDateToAndInputHash(
                restaurantId,
                req.branchId(),
                req.dateFrom(),
                req.dateTo(),
                inputHash
        );

        if (cached.isPresent()) {
            log.debug(
                    "AI summary cache hit for branch {} [{} – {}]",
                    req.branchId(),
                    req.dateFrom(),
                    req.dateTo()
            );

            return toResponse(cached.get(), true);
        }

        // Build prompt and call Gemini.
        String prompt = AiPromptBuilder.build(stripped);

        GeminiClient.GeminiResult result;

        AiUsageLog usageLog = new AiUsageLog();
        usageLog.setRestaurantId(restaurantId);

        try {
            result = gemini.generate(prompt);
            usageLog.setInputTokens(result.inputTokens());
            usageLog.setOutputTokens(result.outputTokens());
            usageLog.setStatus("COMPLETED");
        } catch (RuntimeException ex) {
            // Persist the failure independently so the record survives
            // when this summary transaction rolls back.
            try {
                usageLogService.recordFailure(restaurantId);
            } catch (RuntimeException loggingEx) {
                log.error(
                        "Could not persist failed AI usage log for restaurant {}",
                        restaurantId,
                        loggingEx
                );
            }

            throw ex;
        }

        // Parse and persist the result.
        AiSummary summary = new AiSummary();
        summary.setRestaurantId(restaurantId);
        summary.setBranchId(req.branchId());
        summary.setRequestedBy(userId);
        summary.setDateFrom(req.dateFrom());
        summary.setDateTo(req.dateTo());
        summary.setInputHash(inputHash);
        summary.setCommentCount(stripped.size());
        summary.setResult(result.text());
        summary.setStatus("COMPLETED");

        summaries.save(summary);

        usageLog.setSummaryId(summary.getId());
        usageLogs.save(usageLog);

        return toResponse(summary, false);
    }

    private AiSummaryResponse toResponse(
            AiSummary summary,
            boolean cached
    ) {
        try {
            JsonNode root = mapper.readTree(summary.getResult());

            List<AiSummaryResponse.Finding> praises =
                    parseFindings(root.path("praises"));

            List<AiSummaryResponse.Finding> complaints =
                    parseFindings(root.path("complaints"));

            List<String> issues = new ArrayList<>();
            root.path("recurringIssues")
                    .forEach(n -> issues.add(n.asText()));

            List<String> suggestions = new ArrayList<>();
            root.path("suggestions")
                    .forEach(n -> suggestions.add(n.asText()));

            return new AiSummaryResponse(
                    summary.getId(),
                    summary.getBranchId(),
                    summary.getDateFrom(),
                    summary.getDateTo(),
                    summary.getCommentCount(),
                    cached,
                    summary.getCreatedAt(),
                    root.path("overallSentiment").asText("mixed"),
                    praises,
                    complaints,
                    issues,
                    suggestions
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to parse stored AI result for summary {}",
                    summary.getId(),
                    ex
            );

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not parse AI summary result"
            );
        }
    }

    private List<AiSummaryResponse.Finding> parseFindings(JsonNode array) {
        List<AiSummaryResponse.Finding> findings = new ArrayList<>();

        if (array.isMissingNode()) {
            return findings;
        }

        array.forEach(node -> {
            String finding = node.path("finding").asText("");
            int count = node.path("mentionCount").asInt(0);

            List<String> examples = new ArrayList<>();
            node.path("examples")
                    .forEach(e -> examples.add(e.asText()));

            if (!finding.isBlank()) {
                findings.add(
                        new AiSummaryResponse.Finding(
                                finding,
                                count,
                                examples
                        )
                );
            }
        });

        return findings;
    }

    private static String hash(List<String> comments) {
        try {
            // Sort for deterministic hash regardless of DB ordering.
            List<String> sorted = comments.stream()
                    .sorted()
                    .toList();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            sorted.forEach(c ->
                    digest.update(c.getBytes(StandardCharsets.UTF_8))
            );

            return HexFormat.of().formatHex(digest.digest());

        } catch (Exception e) {
            throw new IllegalStateException(
                    "SHA-256 not available",
                    e
            );
        }
    }
}
