
package com.spiceroute.feedback.admin;

import com.spiceroute.feedback.branch.BranchRequest;
import com.spiceroute.feedback.branch.BranchResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin", description = "Platform super-admin operations")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    /** List all registered restaurants. */
    @GetMapping("/restaurants")
    public List<AdminRestaurantResponse> listRestaurants() {
        return service.listRestaurants();
    }

    /** Platform-wide totals. */
    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return service.platformStats();
    }

    /** Suspend or reactivate a restaurant. */
    @PatchMapping("/restaurants/{id}/status")
    public AdminRestaurantResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRestaurantStatusRequest req) {
        return service.updateStatus(id, req.status());
    }

    /** List all platform users. */
    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        return service.listUsers();
    }

    /** List branches for a selected restaurant. */
    @GetMapping("/restaurants/{restaurantId}/branches")
    public List<BranchResponse> listBranches(
            @PathVariable UUID restaurantId) {
        return service.listBranches(restaurantId);
    }

    /** Create a branch for a selected restaurant. */
    @PostMapping("/restaurants/{restaurantId}/branches")
    public BranchResponse createBranch(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody BranchRequest req) {
        return service.createBranch(restaurantId, req);
    }

    /** Deactivate a branch. */
    @DeleteMapping("/restaurants/{restaurantId}/branches/{branchId}")
    public void deactivateBranch(
            @PathVariable UUID restaurantId,
            @PathVariable UUID branchId) {
        service.deactivateBranch(restaurantId, branchId);
    }
}
