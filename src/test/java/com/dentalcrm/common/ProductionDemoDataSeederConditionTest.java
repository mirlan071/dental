package com.dentalcrm.common;

import com.dentalcrm.user.UserRepository;
import com.dentalcrm.user.Role;
import com.dentalcrm.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ProductionDemoDataSeederConditionTest {
    @Test
    void disabledByDefaultInProduction() {
        try (AnnotationConfigApplicationContext context = context("prod", Map.of())) {
            assertFalse(context.containsBean("productionDemoDataSeeder"));
        }
        try (AnnotationConfigApplicationContext context = context(
                "prod", Map.of("app.load-demo-data", "false"))) {
            assertFalse(context.containsBean("productionDemoDataSeeder"));
        }
    }

    @Test
    void enabledInProductionOnlyWhenExplicitlyTrue() {
        try (AnnotationConfigApplicationContext context = context(
                "prod", Map.of("app.load-demo-data", "true"))) {
            assertTrue(context.containsBean("productionDemoDataSeeder"));
        }
    }

    @Test
    void notEnabledOutsideProductionEvenWhenFlagIsTrue() {
        try (AnnotationConfigApplicationContext context = context(
                "dev", Map.of("app.load-demo-data", "true"))) {
            assertFalse(context.containsBean("productionDemoDataSeeder"));
        }
    }

    @Test
    void seedsWithTheFirstActiveAdmin() throws Exception {
        UserRepository users = mock(UserRepository.class);
        DemoDataSeedService demoDataSeedService = mock(DemoDataSeedService.class);
        User activeAdmin = new User();
        when(users.findFirstByRoleAndActiveTrueOrderByIdAsc(Role.ADMIN))
                .thenReturn(Optional.of(activeAdmin));
        when(demoDataSeedService.seed(activeAdmin))
                .thenReturn(new DemoDataSeedService.SeedSummary(true, 0, 0, 0, 0, 0));

        new ProductionDemoDataSeeder(users, demoDataSeedService).run();

        verify(demoDataSeedService).seed(activeAdmin);
        verify(users, never()).findFirstByRoleOrderByIdAsc(Role.ADMIN);
    }

    private AnnotationConfigApplicationContext context(String profile, Map<String, Object> properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.registerBean(UserRepository.class, () -> mock(UserRepository.class));
        context.registerBean(DemoDataSeedService.class, () -> mock(DemoDataSeedService.class));
        context.register(ProductionDemoDataSeeder.class);
        context.refresh();
        return context;
    }
}
