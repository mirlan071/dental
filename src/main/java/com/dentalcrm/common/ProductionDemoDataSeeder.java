package com.dentalcrm.common;

import com.dentalcrm.user.Role;
import com.dentalcrm.user.User;
import com.dentalcrm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.load-demo-data", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
public class ProductionDemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductionDemoDataSeeder.class);

    private final UserRepository users;
    private final DemoDataSeedService demoDataSeedService;

    public ProductionDemoDataSeeder(UserRepository users, DemoDataSeedService demoDataSeedService) {
        this.users = users;
        this.demoDataSeedService = demoDataSeedService;
    }

    @Override
    public void run(String... args) {
        User admin = users.findFirstByRoleAndActiveTrueOrderByIdAsc(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Production demo data requires an existing active ADMIN account"));

        DemoDataSeedService.SeedSummary summary = demoDataSeedService.seed(admin);
        if (summary.skipped()) {
            log.info("Production demo dataset already present; skipping seed");
            return;
        }
        log.info("Production demo seed completed: doctors created={}, services created={}, patients created={}, appointments created={}, payments created={}",
                summary.doctorsCreated(), summary.servicesCreated(), summary.patientsCreated(),
                summary.appointmentsCreated(), summary.paymentsCreated());
    }
}
