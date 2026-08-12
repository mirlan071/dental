package com.dentalcrm.appointment;

import com.dentalcrm.common.ConflictException;
import com.dentalcrm.doctor.*;
import com.dentalcrm.patient.*;
import com.dentalcrm.payment.*;
import com.dentalcrm.service.*;
import com.dentalcrm.user.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentWorkflowTest {
    private AppointmentRepository appointments;
    private AppointmentServiceItemRepository items;
    private PaymentRepository payments;
    private DoctorRepository doctorRepository;
    private AppointmentManager manager;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        items = mock(AppointmentServiceItemRepository.class);
        payments = mock(PaymentRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        manager = new AppointmentManager(appointments, items, payments, mock(PatientService.class),
                mock(DoctorService.class), mock(ClinicServiceManager.class), mock(UserRepository.class), doctorRepository);
        appointment = appointment(10L, 20L, AppointmentStatus.SCHEDULED);
        when(appointments.findById(10L)).thenReturn(Optional.of(appointment));
        when(items.findByAppointmentId(anyLong())).thenReturn(List.of());
        when(payments.findByAppointmentIdOrderByPaidAt(anyLong())).thenReturn(List.of());
    }

    @Test
    void quantityCalculatesLineTotal() {
        AppointmentServiceItem item = serviceItem("1250.00", 3);
        assertEquals(new BigDecimal("3750.00"), item.lineTotal());
    }

    @Test
    void quantityDefaultsToOne() {
        AppointmentServiceItem item = new AppointmentServiceItem();
        item.setPrice(new BigDecimal("1250.00"));
        assertEquals(1, item.getQuantity());
        assertEquals(new BigDecimal("1250.00"), item.lineTotal());
    }

    @Test
    void servicesTotalSumsPriceTimesQuantity() {
        when(items.findByAppointmentId(10L)).thenReturn(List.of(serviceItem("1000.00", 2), serviceItem("500.00", 3)));
        assertEquals(new BigDecimal("3500.00"), manager.servicesTotal(10L));
    }

    @Test
    void allowsScheduledToInProgress() {
        var response = manager.status(10L, AppointmentStatus.IN_PROGRESS, admin());
        assertEquals(AppointmentStatus.IN_PROGRESS, response.status());
    }

    @Test
    void allowsInProgressToCompletedWhenServicesExist() {
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        when(items.existsByAppointmentId(10L)).thenReturn(true);
        var response = manager.status(10L, AppointmentStatus.COMPLETED, admin());
        assertEquals(AppointmentStatus.COMPLETED, response.status());
    }

    @Test
    void rejectsInvalidTerminalStatusTransition() {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        assertThrows(ConflictException.class,
                () -> manager.status(10L, AppointmentStatus.IN_PROGRESS, admin()));
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    void rejectsCompletingAppointmentWithoutServices() {
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        when(items.existsByAppointmentId(10L)).thenReturn(false);
        ConflictException error = assertThrows(ConflictException.class,
                () -> manager.status(10L, AppointmentStatus.COMPLETED, admin()));
        assertTrue(error.getMessage().contains("without at least one service"));
    }

    @Test
    void doctorCannotAccessAnotherDoctorsAppointment() {
        User doctorUser = appointment(99L, 99L, AppointmentStatus.SCHEDULED).getDoctor().getUser();
        Doctor ownDoctor = new Doctor();
        ownDoctor.setId(99L);
        ownDoctor.setUser(doctorUser);
        when(doctorRepository.findByUserUsername("doctor")).thenReturn(Optional.of(ownDoctor));
        assertThrows(AccessDeniedException.class, () -> manager.find(10L, doctor()));
    }

    @Test
    void doctorCanTransitionOwnAppointment() {
        when(doctorRepository.findByUserUsername("doctor")).thenReturn(Optional.of(appointment.getDoctor()));
        assertEquals(AppointmentStatus.IN_PROGRESS,
                manager.status(10L, AppointmentStatus.IN_PROGRESS, doctor()).status());
    }

    private AppointmentServiceItem serviceItem(String price, int quantity) {
        AppointmentServiceItem item = new AppointmentServiceItem();
        item.setPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }

    private Appointment appointment(Long id, Long doctorId, AppointmentStatus status) {
        User user = new User();
        user.setId(doctorId);
        user.setUsername("doctor");
        user.setFullName("Doctor");
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setUser(user);
        Patient patient = new Patient();
        patient.setId(30L);
        patient.setFullName("Patient");
        patient.setPhone("123");
        Appointment result = new Appointment();
        result.setId(id);
        result.setDoctor(doctor);
        result.setPatient(patient);
        result.setStatus(status);
        result.setStartTime(OffsetDateTime.now().plusDays(1));
        result.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        result.setCreatedBy(user);
        return result;
    }

    private Authentication admin() {
        return new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Authentication doctor() {
        return new UsernamePasswordAuthenticationToken("doctor", "", List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
    }
}
