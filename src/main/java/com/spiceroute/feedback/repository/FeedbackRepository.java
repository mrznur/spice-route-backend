package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    @Query("""
            SELECT f FROM Feedback f
            WHERE f.restaurantId = :restaurantId
              AND (:branchId IS NULL OR f.branchId = :branchId)
              AND (:minRating IS NULL OR f.rating >= :minRating)
              AND (:maxRating IS NULL OR f.rating <= :maxRating)
              AND (:from IS NULL OR f.createdAt >= :from)
              AND (:to   IS NULL OR f.createdAt <= :to)
            ORDER BY f.createdAt DESC
            """)
    Page<Feedback> search(
            @Param("restaurantId") UUID restaurantId,
            @Param("branchId")    UUID branchId,
            @Param("minRating")   Short minRating,
            @Param("maxRating")   Short maxRating,
            @Param("from")        Instant from,
            @Param("to")          Instant to,
            Pageable pageable);

    @Query("""
            SELECT AVG(CAST(f.rating AS double))
            FROM Feedback f
            WHERE f.restaurantId = :restaurantId
              AND (:branchId IS NULL OR f.branchId = :branchId)
            """)
    Double avgRating(@Param("restaurantId") UUID restaurantId, @Param("branchId") UUID branchId);

    long countByRestaurantId(UUID restaurantId);

    long countByRestaurantIdAndBranchId(UUID restaurantId, UUID branchId);
}
