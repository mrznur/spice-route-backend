package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {
    List<DiningTable> findByBranchIdAndRestaurantId(UUID branchId, UUID restaurantId);
    Optional<DiningTable> findByIdAndRestaurantId(UUID id, UUID restaurantId);
    boolean existsByBranchIdAndLabel(UUID branchId, String label);
    long countByBranchIdAndActiveTrue(UUID branchId);
    Optional<DiningTable> findByQrToken(String qrToken);
}
