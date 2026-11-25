package com.spiceroute.feedback.feedback;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Tag(name = "Feedback", description = "Customer feedback submission and dashboard listing")
@RestController
@RequestMapping("/api")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    /** Public — called by the customer QR page. No authentication required. */
    @PostMapping("/public/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@Valid @RequestBody SubmitFeedbackRequest req) {
        return service.submit(req);
    }

    /** Authenticated — paginated feedback list for the dashboard. */
    @GetMapping("/feedback")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public FeedbackPageResponse list(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) Short minRating,
            @RequestParam(required = false) Short maxRating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to   == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return service.list(branchId, minRating, maxRating, fromInstant, toInstant, page, size);
    }

    /** Authenticated — summary stats for performance/dashboard pages. */
    @GetMapping("/feedback/stats")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public FeedbackStatsResponse stats(@RequestParam(required = false) UUID branchId) {
        return service.stats(branchId);
    }
}
