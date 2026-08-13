package com.dentalcrm.doctor;

import com.dentalcrm.user.Role;
import com.dentalcrm.user.User;
import com.dentalcrm.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DoctorApiAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired DoctorRepository doctors;

    private Doctor activeDoctor;
    private Doctor inactiveDoctor;

    @BeforeEach
    void setUp() {
        saveUser("directory-viewer", "Directory Viewer", true);
        activeDoctor = saveDoctor("active-directory", "Active Directory Doctor", "Терапевт", "+996700111222", true);
        inactiveDoctor = saveDoctor("inactive-directory", "Inactive Directory Doctor", "Хирург", "+996700333444", false);
    }

    @Test
    void doctorCannotReadManagementListOrDetails() throws Exception {
        mvc.perform(get("/api/doctors").with(user("directory-viewer").roles("DOCTOR")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/doctors/{id}", inactiveDoctor.getId())
                        .with(user("directory-viewer").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotReadActiveDirectory() throws Exception {
        mvc.perform(get("/api/doctors/active"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void activeDirectoryContainsOnlySafeFieldsAndActiveDoctors() throws Exception {
        mvc.perform(get("/api/doctors/active").with(user("directory-viewer").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(activeDoctor.getId().intValue()))
                .andExpect(jsonPath("$[0].fullName").value("Active Directory Doctor"))
                .andExpect(jsonPath("$[0].specialization").value("Терапевт"))
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].username").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    private Doctor saveDoctor(String username, String fullName, String specialization, String phone, boolean active) {
        Doctor doctor = new Doctor();
        doctor.setUser(saveUser(username, fullName, active));
        doctor.setSpecialization(specialization);
        doctor.setPhone(phone);
        return doctors.saveAndFlush(doctor);
    }

    private User saveUser(String username, String fullName, boolean active) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("unused-in-request-postprocessor");
        user.setFullName(fullName);
        user.setRole(Role.DOCTOR);
        user.setActive(active);
        return users.saveAndFlush(user);
    }
}
