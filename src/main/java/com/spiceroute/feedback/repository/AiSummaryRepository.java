package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.AiSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AiSummaryRepository extends JpaRepository<AiSummary, UUID> {

    /** Cache lookup — same restaurant, branch, date range, and identical comment corpus. */
    Optional<AiSummary> findByRestaurantIdAndBranchIdAndDateFromAndDateToAndInputHash(
            UUID restaurantId, UUID branchId, LocalDate dateFrom, LocalDate dateTo, String inputHash);
}
