package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findByRestaurantIdOrderByNameAsc(UUID restaurantId);
    List<Branch> findByRestaurantIdAndIdInOrderByNameAsc(UUID restaurantId, Collection<UUID> ids);
    Optional<Branch> findByIdAndRestaurantId(UUID id, UUID restaurantId);
    List<Branch> findAllByOrderByNameAsc();
    long countByRestaurantIdAndIdIn(UUID restaurantId, Collection<UUID> ids);
}
