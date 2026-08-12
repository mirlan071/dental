package com.dentalcrm.common;

import com.dentalcrm.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test
    void inactiveDoctorCannotAuthenticate() {
        UserRepository repo = mock(UserRepository.class);
        User user = new User();
        user.setUsername("inactive");
        user.setPasswordHash("hash");
        user.setRole(Role.DOCTOR);
        user.setActive(false);
        when(repo.findByUsernameIgnoreCase("inactive")).thenReturn(Optional.of(user));
        var details = config.userDetailsService(repo).loadUserByUsername("inactive");
        assertFalse(details.isEnabled());
    }

    @Test
    void corsAllowsOnlyConfiguredFrontendWithCredentials() {
        CorsConfigurationSource source = config.corsConfigurationSource("https://clinic.example");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
        var cors = source.getCorsConfiguration(request);
        assertNotNull(cors);
        assertEquals(java.util.List.of("https://clinic.example"), cors.getAllowedOrigins());
        assertEquals(Boolean.TRUE, cors.getAllowCredentials());
        assertFalse(cors.getAllowedOrigins().contains("*"));
    }

    @Test
    void productionCsrfCookieIsSecureAndCrossSiteCapable() {
        var repository = config.csrfTokenRepository(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveToken(repository.generateToken(request), request, response);
        var cookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertTrue(cookie.getSecure());
        assertEquals("None", cookie.getAttribute("SameSite"));
    }

    @Test
    void localCsrfCookieRemainsUsableOverHttp() {
        var repository = config.csrfTokenRepository(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveToken(repository.generateToken(request), request, response);
        var cookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertFalse(cookie.getSecure());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
    }
}
