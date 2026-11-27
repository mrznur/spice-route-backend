
package com.spiceroute.feedback.feedback.analytics;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Validated analytics filter.
 *
 * platformWide = true: SUPER_ADMIN access across all restaurants.
 * platformWide = false: restaurant-scoped access.
 *
 * scope is null for owners (all restaurant branches)
 * and assigned branch IDs for managers (possibly empty).
 */
public record AnalyticsFilter(
        UUID restaurantId,
        boolean platformWide,
        Set<UUID> scope,
        UUID branchId,
        UUID tableId,
        Integer minRating,
        Integer maxRating,
        Instant from,
        Instant toExclusive
) {}
