package com.spiceroute.feedback.security;

import com.spiceroute.feedback.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Tenant guard helper. Every tenant-scoped query must take its restaurant id from
 * {@link #restaurantId()} (the signed token), never from a request parameter or body.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static AuthPrincipal get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return principal;
    }

    public static UUID restaurantId() {
        UUID id = get().restaurantId();
        if (id == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No restaurant context for this account");
        }
        return id;
    }
}
