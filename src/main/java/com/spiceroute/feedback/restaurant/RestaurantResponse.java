package com.spiceroute.feedback.restaurant;

import com.spiceroute.feedback.domain.Restaurant;

import java.time.Instant;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        String name,
        String slug,
        String status,
        Instant createdAt
) {
    public static RestaurantResponse from(Restaurant r) {
        return new RestaurantResponse(
                r.getId(), r.getName(), r.getSlug(),
                r.getStatus().name(), r.getCreatedAt());
    }
}
