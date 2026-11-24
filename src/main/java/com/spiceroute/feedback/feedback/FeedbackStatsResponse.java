package com.spiceroute.feedback.feedback;

public record FeedbackStatsResponse(
        long totalFeedback,
        Double averageRating,     // null when no feedback yet
        long fiveStar,
        long fourStar,
        long threeStar,
        long twoStar,
        long oneStar
) {}
