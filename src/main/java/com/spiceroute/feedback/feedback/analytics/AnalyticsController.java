package com.spiceroute.feedback.feedback.analytics;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Analytics", description = "Rating trends, summaries and branch comparisons")
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID tableId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(branchId, tableId, minRating, maxRating, from, to);
    }

    @GetMapping("/trend")
    public List<TrendPoint> trend(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID tableId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String interval) {
        return service.trend(branchId, tableId, minRating, maxRating, from, to, interval);
    }

    @GetMapping("/branches")
    public List<BranchStat> branches(
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.branches(minRating, maxRating, from, to);
    }
}
