package com.spiceroute.feedback.auth;

import com.spiceroute.feedback.security.AuthPrincipal;
import com.spiceroute.feedback.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public AuthPrincipal me() {
        return CurrentUser.get();
    }

    /** Temporary endpoint to prove role checks work; replaced by the real admin API on Day 4. */
    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Map<String, String> adminPing() {
        return Map.of("message", "super admin access ok");
    }
}
