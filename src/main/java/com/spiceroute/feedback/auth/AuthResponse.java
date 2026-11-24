package com.spiceroute.feedback.auth;

import com.spiceroute.feedback.domain.Role;

import java.util.UUID;

public record AuthResponse(String accessToken, String refreshToken, UserInfo user) {
    public record UserInfo(UUID id, String name, String email, Role role, UUID restaurantId) {}
}
