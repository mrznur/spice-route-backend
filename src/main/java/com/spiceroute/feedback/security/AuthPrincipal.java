package com.spiceroute.feedback.security;

import com.spiceroute.feedback.domain.Role;

import java.util.UUID;

/** The authenticated caller, built from the verified JWT. restaurantId is null only for SUPER_ADMIN. */
public record AuthPrincipal(UUID userId, String email, Role role, UUID restaurantId) {}
