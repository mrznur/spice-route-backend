package com.spiceroute.feedback.feedback.analytics;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Already-validated filter. restaurantId always comes from the token. scope is null for owners
 * (all branches) and the assigned branch ids for managers (possibly empty = sees nothing).
 */
public record AnalyticsFilter(UUID restaurantId, Set<UUID> scope, UUID branchId, UUID tableId,
                              Integer minRating, Integer maxRating, Instant from, Instant toExclusive) {}
