package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.Feedback;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Dynamically builds the WHERE clause so only non-null filters become SQL predicates.
 * This avoids the PostgreSQL "could not determine data type of parameter" error that
 * occurs when untyped nulls are passed to a static JPQL `:param IS NULL` expression.
 */
public final class FeedbackSpecs {

    private FeedbackSpecs() {}

    public static Specification<Feedback> search(
            UUID restaurantId, UUID branchId, Short minRating, Short maxRating,
            Instant from, Instant to) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("restaurantId"), restaurantId));
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branchId"), branchId));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (maxRating != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), maxRating));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
