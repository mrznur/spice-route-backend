package com.spiceroute.feedback.admin;

import com.spiceroute.feedback.admin.AdminRepository.AdminUserRow;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        String role,
        boolean active,
        Instant createdAt,
        String restaurantName
) {
    static AdminUserResponse from(AdminUserRow r) {
        return new AdminUserResponse(
                r.id(), r.name(), r.email(), r.role(),
                r.active(), r.createdAt(), r.restaurantName());
    }
}
