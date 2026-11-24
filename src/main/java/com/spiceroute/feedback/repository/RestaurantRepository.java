package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsBySlug(String slug);

    /** Used by the super-admin API to look up a restaurant by its owner's user id. */
    Optional<Restaurant> findByOwnerId(UUID ownerId);

    /** Used by the super-admin list endpoint; ordered for consistent paging. */
    List<Restaurant> findAllByOrderByCreatedAtDesc();
}
