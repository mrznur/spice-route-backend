
package com.spiceroute.feedback.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.common.SlidingWindowRateLimiter;
import com.spiceroute.feedback.domain.RefreshToken;
import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.domain.User;
import com.spiceroute.feedback.repository.RefreshTokenRepository;
import com.spiceroute.feedback.repository.RestaurantRepository;
import com.spiceroute.feedback.repository.UserRepository;
import com.spiceroute.feedback.security.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@Service
public class AuthService {

    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final SlidingWindowRateLimiter limiter;
    private final RefreshTokenRevocationService revocationService;

    public AuthService(
            UserRepository users,
            RestaurantRepository restaurants,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder encoder,
            JwtService jwt,
            SlidingWindowRateLimiter limiter,
            RefreshTokenRevocationService revocationService
    ) {
        this.users = users;
        this.restaurants = restaurants;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limiter = limiter;
        this.revocationService = revocationService;
    }

    @Transactional
    public AuthResponse registerOwner(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();

        if (users.existsByEmail(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
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

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        String lockKey = "login:" + email;

        // 10 failed attempts within 15 minutes -> locked out
        User user = users.findByEmail(email)
                .orElseThrow(() -> {
                    limiter.recordFailure(lockKey);
                    return new ApiException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid email or password"
                    );
                });

        if (limiter.isLockedOut(lockKey)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in 15 minutes."
            );
        }

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            limiter.recordFailure(lockKey);
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        limiter.reset(lockKey);
        assertCanSignIn(user);

        return respond(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        String raw = req.refreshToken();

        Claims claims;
        try {
            claims = jwt.parse(raw);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token"
            );
        }

        if (!"refresh".equals(claims.get("typ", String.class))) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid refresh token"
            );
        }

        String hash = JwtService.hash(raw);

        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token not recognised"
                ));

        if (stored.isUsed()) {
            // Independent transaction: this revocation survives
            // the Unauthorized exception and outer transaction rollback.
            revocationService.revokeAllForUser(stored.getUserId());

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token already used. All sessions have been invalidated for security."
            );
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token expired"
            );
        }

        stored.setUsed(true);

        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        assertCanSignIn(user);

        return respond(user);
    }

    /**
     * Disabled accounts and suspended restaurants cannot sign in or refresh.
     */
    private void assertCanSignIn(User user) {
        if (!user.isActive()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Account is disabled"
            );
        }

        if (user.getRestaurantId() != null) {
            Restaurant restaurant = restaurants.findById(user.getRestaurantId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.FORBIDDEN,
                            "Restaurant not found"
                    ));

            if (restaurant.getStatus() == RestaurantStatus.SUSPENDED) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        "Restaurant is suspended"
                );
            }
        }
    }

    private AuthResponse respond(User user) {
        String accessToken = jwt.createAccessToken(user);
        String refreshToken = jwt.createRefreshToken(user);

        RefreshToken stored = new RefreshToken();
        stored.setTokenHash(JwtService.hash(refreshToken));
        stored.setUserId(user.getId());
        stored.setExpiresAt(
                Instant.now().plus(jwt.getRefreshDays(), ChronoUnit.DAYS)
        );

        refreshTokens.save(stored);

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getRestaurantId()
                )
        );
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (base.isEmpty()) {
            base = "restaurant";
        }

        String slug = base;

        while (restaurants.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID()
                    .toString().substring(0, 4);
        }

        return slug;
    }
}
