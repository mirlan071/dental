package com.dentalcrm.appointment;

import com.dentalcrm.doctor.*;
import com.dentalcrm.patient.*;
import com.dentalcrm.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AppointmentRepositoryTest {
    @Autowired AppointmentRepository appointments;
    @Autowired UserRepository users;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    private User user;
    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("doc-" + System.nanoTime());
        user.setPasswordHash("hash");
        user.setFullName("Doc");
        user.setRole(Role.DOCTOR);
        users.save(user);
        doctor = new Doctor();
        doctor.setUser(user);
        doctors.save(doctor);
        patient = new Patient();
        patient.setFullName("Patient");
        patient.setPhone("123");
        patients.save(patient);
    }

    @Test
    void detectsOverlappingButNotAdjacentAppointment() {
        OffsetDateTime start = OffsetDateTime.parse("2030-01-01T10:00:00+06:00");
        save(start);
        assertTrue(appointments.hasConflict(doctor.getId(), start.plusMinutes(30), start.plusHours(2), null));
        assertFalse(appointments.hasConflict(doctor.getId(), start.plusHours(1), start.plusHours(2), null));
    }

    @Test
    void dateRangeUsesExclusiveUpperBoundary() {
        OffsetDateTime boundary = OffsetDateTime.parse("2030-01-02T00:00:00+06:00");
        save(boundary);
        assertTrue(appointments.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                boundary.minusDays(1), boundary).isEmpty());
        assertEquals(1, appointments.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                boundary, boundary.plusDays(1)).size());
    }

    @Test
    void futureBlockingQueryIgnoresHistoricalCancelledAndNoShowAppointments() {
        OffsetDateTime moment = OffsetDateTime.parse("2030-01-01T12:00:00+06:00");
        Appointment scheduled = save(moment.plusHours(1), moment.plusHours(2), AppointmentStatus.SCHEDULED);
        Appointment inProgress = save(moment.minusMinutes(30), moment.plusMinutes(30), AppointmentStatus.IN_PROGRESS);
        Appointment completed = save(moment.plusHours(3), moment.plusHours(4), AppointmentStatus.COMPLETED);
        save(moment.minusHours(2), moment, AppointmentStatus.SCHEDULED);
        save(moment.plusHours(5), moment.plusHours(6), AppointmentStatus.CANCELLED);
        save(moment.plusHours(7), moment.plusHours(8), AppointmentStatus.NO_SHOW);

        Set<Long> foundIds = appointments.findByStatusInAndEndTimeAfter(
                        EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.IN_PROGRESS,
                                AppointmentStatus.COMPLETED), moment)
                .stream()
                .map(Appointment::getId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(scheduled.getId(), inProgress.getId(), completed.getId()), foundIds);
    }

    private Appointment save(OffsetDateTime start) {
        return save(start, start.plusHours(1), AppointmentStatus.SCHEDULED);
    }

    private Appointment save(OffsetDateTime start, OffsetDateTime end, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(status);
        appointment.setCreatedBy(user);
        return appointments.saveAndFlush(appointment);
    }
}
