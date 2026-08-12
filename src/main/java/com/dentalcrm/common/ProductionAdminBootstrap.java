package com.dentalcrm.common;

import com.dentalcrm.user.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod")
public class ProductionAdminBootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductionAdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String fullName;

    public ProductionAdminBootstrap(UserRepository users, PasswordEncoder encoder,
                                    @Value("${app.bootstrap-admin.username:}") String username,
                                    @Value("${app.bootstrap-admin.password:}") String password,
                                    @Value("${app.bootstrap-admin.full-name:}") String fullName) {
        this.users = users;
        this.encoder = encoder;
        this.username = username.trim();
        this.password = password;
        this.fullName = fullName.trim();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByRole(Role.ADMIN)) {
            log.info("Production ADMIN already exists; bootstrap skipped");
            return;
        }
        if (username.isBlank() && password.isBlank() && fullName.isBlank()) {
            log.warn("No production ADMIN exists and bootstrap variables are not configured");
            return;
        }
        if (username.isBlank() || password.isBlank() || fullName.isBlank()) {
            throw new IllegalStateException("All APP_BOOTSTRAP_ADMIN_* variables are required to create the initial ADMIN");
        }
        if (password.length() < 10) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must contain at least 10 characters");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("Bootstrap ADMIN username is already used by a non-admin account");
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(encoder.encode(password));
        admin.setFullName(fullName);
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        users.save(admin);
        log.info("Initial production ADMIN account created for username {}", username);
    }
}
