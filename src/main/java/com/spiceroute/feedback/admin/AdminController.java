package com.spiceroute.feedback.admin;

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

    /** B2.2 — List all registered restaurants. */
    @GetMapping("/restaurants")
    public List<AdminRestaurantResponse> listRestaurants() {
        return service.listRestaurants();
    }

    /** B2.3 — Platform-wide totals. */
    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return service.platformStats();
    }

    /** B2.4 — Suspend or reactivate a restaurant. */
    @PatchMapping("/restaurants/{id}/status")
    public AdminRestaurantResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRestaurantStatusRequest req) {
        return service.updateStatus(id, req.status());
    }

    /** B2.5 — List all platform users. */
    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        return service.listUsers();
    }
}
