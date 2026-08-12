package com.dentalcrm.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardAuthorizationTest {
    @Test
    void debtorEndpointsAreAdminOnly() throws Exception {
        var list = DashboardController.class.getMethod("debtors", String.class).getAnnotation(PreAuthorize.class);
        var details = DashboardController.class.getMethod("debtor", Long.class).getAnnotation(PreAuthorize.class);
        assertEquals("hasRole('ADMIN')", list.value());
        assertEquals("hasRole('ADMIN')", details.value());
    }
}
