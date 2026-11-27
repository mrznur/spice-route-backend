
package com.spiceroute.feedback.security;

import com.spiceroute.feedback.domain.Restaurant;
import com.spiceroute.feedback.domain.RestaurantStatus;
import com.spiceroute.feedback.domain.User;
import com.spiceroute.feedback.repository.RestaurantRepository;
import com.spiceroute.feedback.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;
    private final RestaurantRepository restaurants;

    public JwtAuthFilter(
            JwtService jwtService,
            UserRepository users,
            RestaurantRepository restaurants
    ) {
        this.jwtService = jwtService;
        this.users = users;
        this.restaurants = restaurants;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String accessToken = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims claims = jwtService.parse(accessToken);

                if ("access".equals(claims.get("typ", String.class))) {
                    UUID userId = UUID.fromString(claims.getSubject());

                    User user = users.findById(userId).orElse(null);

                    if (user != null && user.isActive() && canAccessRestaurant(user)) {
                        AuthPrincipal principal = new AuthPrincipal(
                                user.getId(),
                                user.getEmail(),
                                user.getRole(),
                                user.getRestaurantId()
                        );

                        var authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_" + principal.role().name()
                                ))
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private boolean canAccessRestaurant(User user) {
        if (user.getRestaurantId() == null) {
            return true;
        }

        Restaurant restaurant = restaurants.findById(user.getRestaurantId())
                .orElse(null);

        return restaurant != null
                && restaurant.getStatus() != RestaurantStatus.SUSPENDED;
    }
}
