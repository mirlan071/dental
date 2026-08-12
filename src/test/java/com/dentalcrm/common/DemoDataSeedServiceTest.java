package com.dentalcrm.common;

import com.dentalcrm.appointment.Appointment;
import com.dentalcrm.appointment.AppointmentRepository;
import com.dentalcrm.appointment.AppointmentServiceItemRepository;
import com.dentalcrm.appointment.AppointmentStatus;
import com.dentalcrm.doctor.DoctorRepository;
import com.dentalcrm.patient.PatientRepository;
import com.dentalcrm.payment.Payment;
import com.dentalcrm.payment.PaymentMethod;
import com.dentalcrm.payment.PaymentRepository;
import com.dentalcrm.service.ClinicServiceRepository;
import com.dentalcrm.user.Role;
import com.dentalcrm.user.User;
import com.dentalcrm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({DemoDataSeedService.class, DemoDataSeedServiceTest.PasswordConfig.class})
class DemoDataSeedServiceTest {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Bishkek");

    private final DemoDataSeedService seedService;
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final ClinicServiceRepository services;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final AppointmentServiceItemRepository appointmentServices;
    private final PaymentRepository payments;
    private final DemoDataSeedMarkerRepository seedMarkers;

    @Autowired
    DemoDataSeedServiceTest(DemoDataSeedService seedService, UserRepository users,
                            DoctorRepository doctors, ClinicServiceRepository services,
                            PatientRepository patients, AppointmentRepository appointments,
                            AppointmentServiceItemRepository appointmentServices,
                            PaymentRepository payments, DemoDataSeedMarkerRepository seedMarkers) {
        this.seedService = seedService;
        this.users = users;
        this.doctors = doctors;
        this.services = services;
        this.patients = patients;
        this.appointments = appointments;
        this.appointmentServices = appointmentServices;
        this.payments = payments;
        this.seedMarkers = seedMarkers;
    }

    @Test
    void seedsRealisticDataOnceAndPreservesExistingAdmin() {
        User existingAdmin = new User();
        existingAdmin.setUsername("clinic-owner");
        existingAdmin.setPasswordHash("existing-bcrypt-hash");
        existingAdmin.setFullName("Владелец клиники");
        existingAdmin.setRole(Role.ADMIN);
        existingAdmin.setActive(true);
        existingAdmin = users.saveAndFlush(existingAdmin);
        Long adminId = existingAdmin.getId();

        DemoDataSeedService.SeedSummary first = seedService.seed(existingAdmin);

        assertFalse(first.skipped());
        assertEquals(6, first.doctorsCreated());
        assertEquals(11, first.servicesCreated());
        assertEquals(50, first.patientsCreated());
        assertEquals(108, first.appointmentsCreated());
        assertEquals(first.appointmentsCreated(), appointments.count());
        assertEquals(first.paymentsCreated(), payments.count());
        assertEquals(1, seedMarkers.count());

        User preservedAdmin = users.findById(adminId).orElseThrow();
        assertEquals("clinic-owner", preservedAdmin.getUsername());
        assertEquals("existing-bcrypt-hash", preservedAdmin.getPasswordHash());
        assertEquals("Владелец клиники", preservedAdmin.getFullName());
        assertEquals(1, users.findAll().stream().filter(user -> user.getRole() == Role.ADMIN).count());

        assertEquals(5, doctors.findAll().stream().filter(doctor -> doctor.getUser().isActive()).count());
        assertEquals(1, doctors.findAll().stream().filter(doctor -> !doctor.getUser().isActive()).count());
        assertEquals(10, services.findAll().stream().filter(service -> service.isActive()).count());
        assertEquals(50, patients.count());
        assertDatasetVariety();

        long usersBefore = users.count();
        long doctorsBefore = doctors.count();
        long servicesBefore = services.count();
        long patientsBefore = patients.count();
        long appointmentsBefore = appointments.count();
        long paymentsBefore = payments.count();

        DemoDataSeedService.SeedSummary second = seedService.seed(existingAdmin);

        assertTrue(second.skipped());
        assertEquals(usersBefore, users.count());
        assertEquals(doctorsBefore, doctors.count());
        assertEquals(servicesBefore, services.count());
        assertEquals(patientsBefore, patients.count());
        assertEquals(appointmentsBefore, appointments.count());
        assertEquals(paymentsBefore, payments.count());
        assertEquals("existing-bcrypt-hash", users.findById(adminId).orElseThrow().getPasswordHash());
    }

    private void assertDatasetVariety() {
        List<Appointment> allAppointments = appointments.findAll();
        Set<AppointmentStatus> statuses = EnumSet.noneOf(AppointmentStatus.class);
        allAppointments.forEach(appointment -> statuses.add(appointment.getStatus()));
        assertTrue(statuses.containsAll(EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)));

        LocalDate today = LocalDate.now(CLINIC_ZONE);
        assertTrue(allAppointments.stream().anyMatch(appointment -> appointment.getStartTime()
                .atZoneSameInstant(CLINIC_ZONE).toLocalDate().isBefore(today)));
        assertTrue(allAppointments.stream().anyMatch(appointment -> appointment.getStartTime()
                .atZoneSameInstant(CLINIC_ZONE).toLocalDate().equals(today)));
        assertTrue(allAppointments.stream().anyMatch(appointment -> appointment.getStartTime()
                .atZoneSameInstant(CLINIC_ZONE).toLocalDate().isAfter(today)));

        Set<PaymentMethod> methods = EnumSet.noneOf(PaymentMethod.class);
        payments.findAll().forEach(payment -> methods.add(payment.getPaymentMethod()));
        assertEquals(EnumSet.allOf(PaymentMethod.class), methods);
        assertTrue(payments.findAll().stream().anyMatch(payment ->
                payment.getPaidAt().isAfter(payment.getAppointment().getEndTime())));

        int fullyPaid = 0;
        int partiallyPaid = 0;
        int unpaid = 0;
        Set<Long> debtorPatients = new HashSet<>();
        Map<Long, Set<Long>> doctorsByPatient = new HashMap<>();
        for (Appointment appointment : allAppointments) {
            doctorsByPatient.computeIfAbsent(appointment.getPatient().getId(), ignored -> new HashSet<>())
                    .add(appointment.getDoctor().getId());
            if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
                continue;
            }
            BigDecimal total = appointmentServices.findByAppointmentId(appointment.getId()).stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paid = payments.findByAppointmentIdOrderByPaidAt(appointment.getId()).stream()
                    .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            int comparison = paid.compareTo(total);
            if (comparison == 0) {
                fullyPaid++;
            } else if (paid.signum() == 0) {
                unpaid++;
                debtorPatients.add(appointment.getPatient().getId());
            } else {
                partiallyPaid++;
                debtorPatients.add(appointment.getPatient().getId());
            }
        }
        assertTrue(fullyPaid > 0);
        assertTrue(partiallyPaid > 0);
        assertTrue(unpaid > 0);
        assertTrue(debtorPatients.size() >= 10 && debtorPatients.size() <= 15);
        assertTrue(doctorsByPatient.values().stream().anyMatch(doctorIds -> doctorIds.size() > 1));
    }

    @TestConfiguration
    static class PasswordConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }
}
