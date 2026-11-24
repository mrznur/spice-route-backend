package com.spiceroute.feedback.ai;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Insights", description = "AI-powered feedback analysis and summaries")
@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class AiController {

    private final AiSummaryService service;

    public AiController(AiSummaryService service) {
        this.service = service;
    }

    /**
     * B3.11 — Generate (or return cached) an AI summary for a branch's feedback.
     * Requires at least 5 meaningful comments in the date range.
     * Managers are limited to their assigned branches (enforced in AiSummaryService via BranchService).
     */
    @PostMapping("/summary")
    public AiSummaryResponse summarise(@Valid @RequestBody AiSummaryRequest req) {
        return service.summarise(req);
    }
}
