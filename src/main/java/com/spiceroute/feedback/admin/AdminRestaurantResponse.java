package com.spiceroute.feedback.admin;

import com.spiceroute.feedback.admin.AdminRepository.AdminRestaurantRow;

import java.time.Instant;
import java.util.UUID;

public record AdminRestaurantResponse(
        UUID id,
        String name,
        String slug,
        String status,
        Instant createdAt,
        String ownerName,
        String ownerEmail,
        long branchCount,
        long feedbackCount
) {
    static AdminRestaurantResponse from(AdminRestaurantRow r) {
        return new AdminRestaurantResponse(
                r.id(), r.name(), r.slug(), r.status(), r.createdAt(),
                r.ownerName(), r.ownerEmail(), r.branchCount(), r.feedbackCount());
    }
}
