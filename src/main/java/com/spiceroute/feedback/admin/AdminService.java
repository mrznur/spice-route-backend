package com.spiceroute.feedback.admin;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.repository.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final AdminRepository adminRepo;
    private final RestaurantRepository restaurants;

    public AdminService(AdminRepository adminRepo, RestaurantRepository restaurants) {
        this.adminRepo = adminRepo;
        this.restaurants = restaurants;
    }

    /** B2.2 — All restaurants with owner info, branch count, feedback count. */
    @Transactional(readOnly = true)
    public List<AdminRestaurantResponse> listRestaurants() {
        return adminRepo.listRestaurants().stream()
                .map(AdminRestaurantResponse::from)
                .toList();
    }

    /** B2.3 — Platform-wide totals. */
    @Transactional(readOnly = true)
    public AdminStatsResponse platformStats() {
        AdminRepository.AdminStatsRow row = adminRepo.platformStats();
        return new AdminStatsResponse(
                row.restaurantCount(),
                row.userCount(),
                row.feedbackTotal(),
                row.feedbackToday());
    }

    /** B2.4 — Set a restaurant's status to ACTIVE or SUSPENDED. */
    @Transactional
    public AdminRestaurantResponse updateStatus(UUID restaurantId, String status) {
        Restaurant restaurant = restaurants.findById(restaurantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        restaurant.setStatus(RestaurantStatus.valueOf(status));
        restaurants.save(restaurant);
        // Re-query to get the aggregated counts (branches, feedback) in the response
        return adminRepo.listRestaurants().stream()
                .filter(r -> r.id().equals(restaurantId))
                .findFirst()
                .map(AdminRestaurantResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reload restaurant"));
    }

    /** B2.5 — All platform users with their restaurant name. */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return adminRepo.listUsers().stream()
                .map(AdminUserResponse::from)
                .toList();
    }
}
