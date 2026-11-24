package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    List<User> findByRestaurantIdAndRoleOrderByNameAsc(UUID restaurantId, Role role);
    Optional<User> findByIdAndRestaurantIdAndRole(UUID id, UUID restaurantId, Role role);
}
