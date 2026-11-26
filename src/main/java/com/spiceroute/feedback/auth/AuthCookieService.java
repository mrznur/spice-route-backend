
package com.spiceroute.feedback.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    private static final String ACCESS_COOKIE = "accessToken";
    private static final String REFRESH_COOKIE = "refreshToken";

    private final boolean secure;
    private final String sameSite;
    private final long accessMinutes;
    private final long refreshDays;

    public AuthCookieService(
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.same-site:Strict}") String sameSite,
            @Value("${app.jwt.access-minutes}") long accessMinutes,
            @Value("${app.jwt.refresh-days}") long refreshDays
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    public void addAuthCookies(
            HttpHeaders headers,
            String accessToken,
            String refreshToken
    ) {
        headers.add(HttpHeaders.SET_COOKIE,
                createCookie(ACCESS_COOKIE, accessToken,
                        Duration.ofMinutes(accessMinutes)).toString());

        headers.add(HttpHeaders.SET_COOKIE,
                createCookie(REFRESH_COOKIE, refreshToken,
                        Duration.ofDays(refreshDays)).toString());
    }

    public void clearAuthCookies(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE,
                createCookie(ACCESS_COOKIE, "", Duration.ZERO).toString());

        headers.add(HttpHeaders.SET_COOKIE,
                createCookie(REFRESH_COOKIE, "", Duration.ZERO).toString());
    }

    private ResponseCookie createCookie(
            String name,
            String value,
            Duration maxAge
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
