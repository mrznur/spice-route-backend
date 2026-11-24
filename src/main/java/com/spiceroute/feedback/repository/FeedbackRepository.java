package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface FeedbackRepository
        extends JpaRepository<Feedback, UUID>, JpaSpecificationExecutor<Feedback> {

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
