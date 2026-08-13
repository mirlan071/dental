package com.dentalcrm.common;

import com.dentalcrm.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductionAdminBootstrapTest {
    @Test
    void doesNothingWithoutExplicitConfiguration() {
        UserRepository users = mock(UserRepository.class);
        new ProductionAdminBootstrap(users, mock(PasswordEncoder.class), "", "", "").run();
        verify(users, never()).save(any());
    }

    @Test
    void doesNothingWhenAdminAlreadyExists() {
        UserRepository users = mock(UserRepository.class);
        when(users.existsByRoleAndActiveTrue(Role.ADMIN)).thenReturn(true);
        new ProductionAdminBootstrap(users, mock(PasswordEncoder.class), "owner", "very-secure-password", "Owner").run();
        verify(users, never()).save(any());
    }

    @Test
    void inactiveAdminDoesNotPreventConfiguredBootstrap() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User inactive = new User();
        inactive.setUsername("owner");
        inactive.setRole(Role.ADMIN);
        inactive.setActive(false);
        inactive.setAuthVersion(4);
        when(users.existsByRoleAndActiveTrue(Role.ADMIN)).thenReturn(false);
        when(users.findByUsernameIgnoreCase("owner")).thenReturn(java.util.Optional.of(inactive));
        when(encoder.encode("very-secure-password")).thenReturn("bcrypt-hash");
        new ProductionAdminBootstrap(users, encoder, "owner", "very-secure-password", "Owner").run();
        assertTrue(inactive.isActive());
        assertEquals("bcrypt-hash", inactive.getPasswordHash());
        assertEquals("Owner", inactive.getFullName());
        assertEquals(5, inactive.getAuthVersion());
        verify(users, never()).save(any());
    }

    @Test
    void createsBcryptBackedAdminOnlyWhenExplicitlyConfigured() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("very-secure-password")).thenReturn("bcrypt-hash");
        new ProductionAdminBootstrap(users, encoder, "owner", "very-secure-password", "Clinic Owner").run();
        verify(users).save(argThat(user -> user.getRole() == Role.ADMIN && user.isActive()
                && user.getUsername().equals("owner") && user.getPasswordHash().equals("bcrypt-hash")));
    }

    @Test
    void rejectsPartialConfiguration() {
        UserRepository users = mock(UserRepository.class);
        assertThrows(IllegalStateException.class,
                () -> new ProductionAdminBootstrap(users, mock(PasswordEncoder.class), "owner", "", "Owner").run());
    }
}
