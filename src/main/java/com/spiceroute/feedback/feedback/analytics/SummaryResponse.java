package com.spiceroute.feedback.feedback.analytics;

import java.util.Map;

/** averageRating is null when there is no feedback; ratingCounts has keys 1..5. */
public record SummaryResponse(long total, Double averageRating, long withComments,
                              Map<Integer, Long> ratingCounts) {}
