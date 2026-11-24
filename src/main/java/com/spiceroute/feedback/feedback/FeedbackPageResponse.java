package com.spiceroute.feedback.feedback;

import java.util.List;

public record FeedbackPageResponse(
        List<FeedbackResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
