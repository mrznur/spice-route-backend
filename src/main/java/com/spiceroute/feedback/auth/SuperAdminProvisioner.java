
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

@Component
@Profile("prod")
public class SuperAdminProvisioner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(SuperAdminProvisioner.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public SuperAdminProvisioner(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${SUPERADMIN_EMAIL:}") String email,
            @Value("${SUPERADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("provision-super-admin")) {
            return;
        }

        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "SUPERADMIN_EMAIL and SUPERADMIN_PASSWORD are required."
            );
        }

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "Superadmin password must contain at least 12 characters."
            );
        }

        if (users.existsByRole(Role.SUPER_ADMIN)) {
            throw new IllegalStateException(
                    "A SUPER_ADMIN already exists. Provisioning refused."
            );
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException(
                    "The supplied email already belongs to another account."
            );
        }

        User admin = new User();
        admin.setName("Platform Admin");
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.SUPER_ADMIN);

        users.save(admin);

        log.info("Successfully provisioned the initial platform administrator.");
    }
}
