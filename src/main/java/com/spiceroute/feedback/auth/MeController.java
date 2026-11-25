package com.spiceroute.feedback.auth;

import com.spiceroute.feedback.security.AuthPrincipal;
import com.spiceroute.feedback.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Register, login and refresh tokens")
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public AuthPrincipal me() {
        return CurrentUser.get();
    }
}
