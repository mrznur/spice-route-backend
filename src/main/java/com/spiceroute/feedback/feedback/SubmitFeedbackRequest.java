package com.spiceroute.feedback.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitFeedbackRequest(
        @NotBlank @Size(max = 64) String qrToken,
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5") Integer rating,
        @Size(max = 1000, message = "Comment must be at most 1000 characters") String comment,
        @Size(max = 150) String customerName,
        @Pattern(regexp = "^$|^[0-9+()\\- ]{6,30}$", message = "Invalid phone number") String customerPhone,
        @Size(max = 10) String language,   // optional hint; the comment text is detected server-side
        Boolean anonymous                   // optional; true = never store name or phone
) {}
