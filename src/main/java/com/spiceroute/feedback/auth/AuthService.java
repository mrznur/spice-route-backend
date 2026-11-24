package com.spiceroute.feedback.auth;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.domain.User;
import com.spiceroute.feedback.repository.RestaurantRepository;
import com.spiceroute.feedback.repository.UserRepository;
import com.spiceroute.feedback.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, RestaurantRepository restaurants,
                       PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.restaurants = restaurants;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse registerOwner(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(req.restaurantName().trim());
        restaurant.setSlug(uniqueSlug(req.restaurantName()));
        restaurant = restaurants.save(restaurant);

        User owner = new User();
        owner.setName(req.name().trim());
        owner.setEmail(email);
        owner.setPasswordHash(encoder.encode(req.password()));
        owner.setRole(Role.OWNER);
        owner.setRestaurantId(restaurant.getId());
        owner = users.save(owner);

        restaurant.setOwnerId(owner.getId());
        return respond(owner);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        assertCanSignIn(user);
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest req) {
        Claims claims;
        try {
            claims = jwt.parse(req.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        if (!"refresh".equals(claims.get("typ", String.class))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        User user = users.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        assertCanSignIn(user);
        return respond(user);
    }

    /** Disabled accounts and suspended restaurants cannot sign in or refresh. */
    private void assertCanSignIn(User user) {
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        if (user.getRestaurantId() != null) {
            Restaurant restaurant = restaurants.findById(user.getRestaurantId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Restaurant not found"));
            if (restaurant.getStatus() == RestaurantStatus.SUSPENDED) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Restaurant is suspended");
            }
        }
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(
                jwt.createAccessToken(user),
                jwt.createRefreshToken(user),
                new AuthResponse.UserInfo(user.getId(), user.getName(), user.getEmail(),
                        user.getRole(), user.getRestaurantId()));
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = "restaurant";
        String slug = base;
        while (restaurants.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 4);
        }
        return slug;
    }
}
