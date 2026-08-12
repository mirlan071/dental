package com.dentalcrm.common;

import com.dentalcrm.user.Role;
import com.dentalcrm.user.User;
import com.dentalcrm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final DemoDataSeedService demoDataSeedService;

    public DevDataSeeder(UserRepository users, PasswordEncoder encoder, DemoDataSeedService demoDataSeedService) {
        this.users = users;
        this.encoder = encoder;
        this.demoDataSeedService = demoDataSeedService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = users.findByUsernameIgnoreCase("admin").orElseGet(() -> users.save(
                user("admin", "admin123", "Администратор клиники", Role.ADMIN, true)));

        if (users.existsByUsernameIgnoreCase("__demo_dataset_v1__")) {
            log.info("Realistic development demo dataset already present; skipping seed");
            return;
        }

        DemoDataSeedService.SeedSummary summary = demoDataSeedService.seed(admin);
        if (summary.skipped()) {
            log.info("Realistic development demo dataset already present; skipping seed");
            return;
        }
        log.info("Development demo seed completed: {} doctors, {} services, {} patients, {} appointments, {} payments",
                summary.doctorsCreated(), summary.servicesCreated(), summary.patientsCreated(),
                summary.appointmentsCreated(), summary.paymentsCreated());
    }

    private User user(String username, String password, String name, Role role, boolean active) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setFullName(name);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
