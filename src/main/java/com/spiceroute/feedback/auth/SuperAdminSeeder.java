
package com.spiceroute.feedback.auth;

import com.spiceroute.feedback.domain.Role;
import com.spiceroute.feedback.domain.User;
import com.spiceroute.feedback.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first platform admin on startup in the local profile only.
 */
@Component
@Profile("local")
public class SuperAdminSeeder implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public SuperAdminSeeder(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${app.superadmin.email}") String email,
            @Value("${app.superadmin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.SUPER_ADMIN)) {
            log.info("A platform admin already exists. Skipping local seeding.");
            return;
        }

        User admin = new User();
        admin.setName("Platform Admin");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.SUPER_ADMIN);

        users.save(admin);

        log.info("Created local platform admin '{}'.", email);
    }
}
