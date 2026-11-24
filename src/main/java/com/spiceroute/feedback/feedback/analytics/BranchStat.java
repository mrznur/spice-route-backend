package com.spiceroute.feedback.feedback.analytics;

import java.util.Map;
import java.util.UUID;

public record BranchStat(UUID branchId, String branchName, long count, Double averageRating,
                         Map<Integer, Long> ratingCounts) {}
