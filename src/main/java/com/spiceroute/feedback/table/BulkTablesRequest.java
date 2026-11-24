package com.spiceroute.feedback.table;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Creates "{prefix} 1" ... "{prefix} {count}", skipping labels that already exist. */
public record BulkTablesRequest(
        @Min(value = 1, message = "count must be between 1 and 100")
        @Max(value = 100, message = "count must be between 1 and 100") int count,
        @Size(max = 40) String prefix) {}
