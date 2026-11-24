package com.spiceroute.feedback.ai;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

public record AiSummaryRequest(
        @NotNull UUID branchId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
) {}
