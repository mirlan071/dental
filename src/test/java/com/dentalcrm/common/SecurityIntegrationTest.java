package com.dentalcrm.common;

import com.dentalcrm.doctor.*;
import com.dentalcrm.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DoctorRepository doctors;
    @Autowired SessionRegistry sessions;
    @Autowired PasswordEncoder encoder;
    private Long doctorId;

    @BeforeEach
    void createUsers() {
        clearSessions();
        doctors.deleteAll();
        users.deleteAll();
        users.save(user("security-admin", "Security Admin", Role.ADMIN));
        User doctorUser = users.save(user("security-doctor", "Security Doctor", Role.DOCTOR));
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctorId = doctors.save(doctor).getId();
    }

    @AfterEach
    void cleanUp() {
        clearSessions();
        doctors.deleteAll();
        users.deleteAll();
    }

    @Test
    void loginRequiresCsrfAndRotatesExistingSessionId() throws Exception {
        MockHttpSession anonymousSession = new MockHttpSession();
        String originalId = anonymousSession.getId();
        String body = "{\"username\":\"security-admin\",\"password\":\"password123\"}";

        mvc.perform(post("/api/auth/login").session(anonymousSession)
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/login").session(anonymousSession).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", not(org.hamcrest.Matchers.nullValue())));
        Assertions.assertNotEquals(originalId, anonymousSession.getId());
        SecurityContext context = (SecurityContext) anonymousSession.getAttribute("SPRING_SECURITY_CONTEXT");
        Assertions.assertNull(((org.springframework.security.core.userdetails.UserDetails)
                context.getAuthentication().getPrincipal()).getPassword());
    }

    @Test
    void validationErrorsAreReturnedInRussian() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType("application/json").content("{\"username\":\"\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Проверьте заполнение полей."))
                .andExpect(jsonPath("$.validationErrors.username").value("Укажите имя пользователя."));
    }

    @Test
    void deactivatedUserLosesExistingSession() throws Exception {
        MockHttpSession session = login("security-admin", "password123");
        User user = users.findByUsernameIgnoreCase("security-admin").orElseThrow();
        user.setActive(false);
        users.saveAndFlush(user);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
        Assertions.assertTrue(session.isInvalid());
    }

    @Test
    void passwordResetRevokesEveryExistingDoctorSession() throws Exception {
        MockHttpSession firstDoctorSession = login("security-doctor", "password123");
        MockHttpSession secondDoctorSession = login("security-doctor", "password123");
        MockHttpSession sessionMissedByRegistry = login("security-doctor", "password123");
        sessions.removeSessionInformation(sessionMissedByRegistry.getId());
        MockHttpSession adminSession = login("security-admin", "password123");

        mvc.perform(patch("/api/doctors/{id}/password", doctorId).session(adminSession).with(csrf())
                        .contentType("application/json").content("{\"password\":\"newpassword123\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(firstDoctorSession))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(secondDoctorSession))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(sessionMissedByRegistry))
                .andExpect(status().isUnauthorized());
        Assertions.assertTrue(firstDoctorSession.isInvalid());
        Assertions.assertTrue(secondDoctorSession.isInvalid());
        Assertions.assertTrue(sessionMissedByRegistry.isInvalid());
        login("security-doctor", "newpassword123");
    }

    @Test
    void deactivateThenReactivateCannotRestoreOldDoctorSession() throws Exception {
        MockHttpSession doctorSession = login("security-doctor", "password123");
        sessions.removeSessionInformation(doctorSession.getId());
        MockHttpSession adminSession = login("security-admin", "password123");

        mvc.perform(patch("/api/doctors/{id}/active", doctorId).session(adminSession).with(csrf())
                        .contentType("application/json").content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/doctors/{id}/active", doctorId).session(adminSession).with(csrf())
                        .contentType("application/json").content("{\"active\":true}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").session(doctorSession))
                .andExpect(status().isUnauthorized());
        Assertions.assertTrue(doctorSession.isInvalid());
        login("security-doctor", "password123");
    }

    private User user(String username, String fullName, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode("password123"));
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private MockHttpSession login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        var result = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void clearSessions() {
        sessions.getAllPrincipals().forEach(principal -> sessions.getAllSessions(principal, true)
                .forEach(session -> sessions.removeSessionInformation(session.getSessionId())));
    }
}
