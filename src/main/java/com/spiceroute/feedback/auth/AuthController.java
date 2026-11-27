
package com.spiceroute.feedback.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Auth", description = "Register, login, refresh and logout")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/register-owner")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse.UserInfo> register(
            @Valid @RequestBody RegisterRequest req
    ) {
        AuthResponse response = authService.registerOwner(req);

        HttpHeaders headers = new HttpHeaders();
        authCookieService.addAuthCookies(
                headers,
                response.accessToken(),
                response.refreshToken()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(response.user());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse.UserInfo> login(
            @Valid @RequestBody LoginRequest req
    ) {
        AuthResponse response = authService.login(req);

        HttpHeaders headers = new HttpHeaders();
        authCookieService.addAuthCookies(
                headers,
                response.accessToken(),
                response.refreshToken()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.user());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse.UserInfo> refresh(
            @CookieValue(name = "refreshToken", required = false)
            String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            HttpHeaders headers = new HttpHeaders();
            authCookieService.clearAuthCookies(headers);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .headers(headers)
                    .build();
        }

        AuthResponse response = authService.refresh(
                new RefreshRequest(refreshToken)
        );

        HttpHeaders headers = new HttpHeaders();
        authCookieService.addAuthCookies(
                headers,
                response.accessToken(),
                response.refreshToken()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false)
            String refreshToken
    ) {
        authService.logout(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        authCookieService.clearAuthCookies(headers);

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
    }
}
