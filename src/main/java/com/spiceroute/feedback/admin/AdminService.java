
package com.spiceroute.feedback.admin;

import com.spiceroute.feedback.branch.BranchRequest;
import com.spiceroute.feedback.repository.BranchRepository;
import com.spiceroute.feedback.branch.BranchResponse;
import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Branch;
import com.spiceroute.feedback.domain.DiningTable;
import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.repository.DiningTableRepository;
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
    private final BranchRepository branches;
    private final DiningTableRepository tables;

    public AdminService(
            AdminRepository adminRepo,
            RestaurantRepository restaurants,
            BranchRepository branches,
            DiningTableRepository tables
    ) {
        this.adminRepo = adminRepo;
        this.restaurants = restaurants;
        this.branches = branches;
        this.tables = tables;
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
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Restaurant not found"));

        restaurant.setStatus(RestaurantStatus.valueOf(status));
        restaurants.save(restaurant);

        return adminRepo.listRestaurants().stream()
                .filter(r -> r.id().equals(restaurantId))
                .findFirst()
                .map(AdminRestaurantResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to reload restaurant"));
    }

    /** B2.5 — All platform users with their restaurant name. */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return adminRepo.listUsers().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    /** List branches belonging to a selected restaurant. */
    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(UUID restaurantId) {
        requireRestaurant(restaurantId);

        return branches.findByRestaurantIdOrderByNameAsc(restaurantId)
                .stream()
                .map(BranchResponse::from)
                .toList();
    }

    /** Create a branch for a selected restaurant. */
    @Transactional
    public BranchResponse createBranch(UUID restaurantId, BranchRequest req) {
        requireRestaurant(restaurantId);

        Branch branch = new Branch();
        branch.setRestaurantId(restaurantId);
        branch.setName(req.name().trim());
        branch.setAddress(
                req.address() == null ? null : req.address().trim()
        );

        return BranchResponse.from(branches.save(branch));
    }

    /** Deactivate a branch without allowing deletion of active tables. */
    @Transactional
    public void deactivateBranch(UUID restaurantId, UUID branchId) {
        Branch branch = branches.findByIdAndRestaurantId(
                        branchId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Branch not found"));

        long activeTables =
                tables.countByBranchIdAndActiveTrue(branch.getId());

        if (activeTables > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Branch has " + activeTables
                            + " active table(s). Deactivate all tables first."
            );
        }

        branch.setActive(false);
    }

    private Restaurant requireRestaurant(UUID restaurantId) {
        return restaurants.findById(restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Restaurant not found"));
    }
}
