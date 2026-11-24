package com.spiceroute.feedback.security;

import com.spiceroute.feedback.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Created inside SecurityConfig (not a @Component) so it only runs once, in the security chain. */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                if ("access".equals(claims.get("typ", String.class))) {
                    String rid = claims.get("rid", String.class);
                    AuthPrincipal principal = new AuthPrincipal(
                            UUID.fromString(claims.getSubject()),
                            claims.get("email", String.class),
                            Role.valueOf(claims.get("role", String.class)),
                            rid == null ? null : UUID.fromString(rid));
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // invalid or expired token: leave the request unauthenticated (-> 401)
            }
        }
        chain.doFilter(request, response);
    }
}
