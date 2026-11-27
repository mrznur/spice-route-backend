
package com.spiceroute.feedback.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.spiceroute.feedback.domain.Feedback;

import jakarta.persistence.criteria.Predicate;

public final class FeedbackSpecs {

    private FeedbackSpecs() {
        // Utility class
    }

    public static Specification<Feedback> searchAllBranches(
            UUID restaurantId,
            UUID branchId,
            Short minRating,
            Short maxRating,
            Instant from,
            Instant toExclusive
    ) {
        return search(
                restaurantId, null, branchId,
                minRating, maxRating, from, toExclusive
        );
    }

    /**
     * Platform-wide search. Use only after SUPER_ADMIN authorization.
     */
    public static Specification<Feedback> searchPlatformWide(
            UUID branchId,
            Short minRating,
            Short maxRating,
            Instant from,
            Instant toExclusive
    ) {
        return build(
                null, null, branchId,
                minRating, maxRating, from, toExclusive,
                false
        );
    }

    /**
     * Tenant-scoped search.
     *
     * allowedBranchIds:
     * null      -> All branches within the restaurant
     * empty     -> No branches
     * non-empty -> Only authorized branches
     */
    public static Specification<Feedback> search(
            UUID restaurantId,
            Set<UUID> allowedBranchIds,
            UUID branchId,
            Short minRating,
            Short maxRating,
            Instant from,
            Instant toExclusive
    ) {
        return build(
                restaurantId, allowedBranchIds, branchId,
                minRating, maxRating, from, toExclusive,
                true
        );
    }

    private static Specification<Feedback> build(
            UUID restaurantId,
            Set<UUID> allowedBranchIds,
            UUID branchId,
            Short minRating,
            Short maxRating,
            Instant from,
            Instant toExclusive,
            boolean filterRestaurant
    ) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (filterRestaurant) {
                predicates.add(
                        cb.equal(root.get("restaurantId"), restaurantId)
                );
            }

            if (allowedBranchIds != null) {
                if (allowedBranchIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(
                            root.get("branchId").in(allowedBranchIds)
                    );
                }
            }

            if (branchId != null) {
                predicates.add(
                        cb.equal(root.get("branchId"), branchId)
                );
            }

            if (minRating != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("rating"), minRating)
                );
            }

            if (maxRating != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("rating"), maxRating)
                );
            }

            if (from != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"), from)
                );
            }

            if (toExclusive != null) {
                predicates.add(
                        cb.lessThan(
                                root.get("createdAt"), toExclusive)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
