package com.spiceroute.feedback.table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TableRequest(@NotBlank @Size(max = 50) String label, Boolean active) {}
