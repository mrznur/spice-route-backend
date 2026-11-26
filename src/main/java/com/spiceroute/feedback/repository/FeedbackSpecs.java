
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

    /**
     * Search feedback across all branches of a restaurant.
     *
     * IMPORTANT:
     * Use only after restaurant-owner authorization
     * has already been verified.
     */
    public static Specification<Feedback> searchAllBranches(
            UUID restaurantId,
            UUID branchId,
            Short minRating,
            Short maxRating,
            Instant from,
            Instant toExclusive
    ) {
        return search(
                restaurantId,
                null,
                branchId,
                minRating,
                maxRating,
                from,
                toExclusive
        );
    }

    /**
     * Search feedback within an authorized branch scope.
     *
     * allowedBranchIds:
     * null      -> All branches (owner-only usage)
     * empty     -> No branches (manager has no access)
     * non-empty -> Only authorized branches
     *
     * toExclusive is an exclusive upper date boundary.
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

        return (root, query, cb) -> {

            var predicates = new ArrayList<Predicate>();

            // Restaurant-level isolation
            predicates.add(
                    cb.equal(root.get("restaurantId"), restaurantId)
            );

            // Branch-level authorization
            if (allowedBranchIds != null) {

                if (allowedBranchIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(
                            root.get("branchId").in(allowedBranchIds)
                    );
                }
            }

            // Optional branch filter
            if (branchId != null) {
                predicates.add(
                        cb.equal(root.get("branchId"), branchId)
                );
            }

            // Rating filters
            if (minRating != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("rating"),
                                minRating
                        )
                );
            }

            if (maxRating != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("rating"),
                                maxRating
                        )
                );
            }

            // Date filters
            if (from != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                from
                        )
                );
            }

            if (toExclusive != null) {
                predicates.add(
                        cb.lessThan(
                                root.get("createdAt"),
                                toExclusive
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
