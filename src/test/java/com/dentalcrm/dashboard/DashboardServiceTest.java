package com.dentalcrm.dashboard;

import com.dentalcrm.appointment.*;
import com.dentalcrm.doctor.*;
import com.dentalcrm.patient.*;
import com.dentalcrm.payment.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private PatientRepository patients;
    private AppointmentRepository appointments;
    private PaymentRepository payments;
    private DoctorRepository doctors;
    private DashboardService service;
    private Doctor doctor;
    private OffsetDateTime from;
    private OffsetDateTime to;

    @BeforeEach
    void setUp() {
        patients = mock(PatientRepository.class);
        appointments = mock(AppointmentRepository.class);
        payments = mock(PaymentRepository.class);
        doctors = mock(DoctorRepository.class);
        service = new DashboardService(patients, appointments, payments, doctors);
        doctor = new Doctor();
        doctor.setId(7L);
        from = OffsetDateTime.parse("2026-08-10T00:00:00+06:00");
        to = from.plusDays(7);
        when(doctors.findByUserUsername("doctor")).thenReturn(Optional.of(doctor));
        when(appointments.findByDoctorIdAndStartTimeBetweenOrderByStartTime(7L, from, to)).thenReturn(List.of());
        when(payments.findByAppointmentDoctorIdAndPaidAtBetween(7L, from, to)).thenReturn(List.of());
    }

    @Test
    void clinicSummarySeparatesQrRevenue() {
        when(patients.count()).thenReturn(3L);
        when(appointments.findByStartTimeBetweenOrderByStartTime(from, to)).thenReturn(List.of());
        when(payments.findByPaidAtBetween(from, to)).thenReturn(List.of(payment("1200.00", PaymentMethod.QR), payment("500.00", PaymentMethod.CASH)));
        var result = service.summary(from, to);
        assertEquals(new BigDecimal("1200.00"), result.qrRevenue());
        assertEquals(new BigDecimal("1700.00"), result.clinicRevenue());
    }

    @Test
    void doctorAnalyticsUsesAuthenticatedDoctorsDataOnly() {
        Patient first = patient(1L);
        Patient second = patient(2L);
        when(appointments.findByDoctorIdAndStartTimeBetweenOrderByStartTime(7L, from, to))
                .thenReturn(List.of(appointment(first, AppointmentStatus.COMPLETED), appointment(first, AppointmentStatus.SCHEDULED), appointment(second, AppointmentStatus.CANCELLED)));
        var result = service.doctorSummary("doctor", from, to);
        assertEquals(1, result.patients());
        assertEquals(1, result.completedAppointments());
        verify(appointments).findByDoctorIdAndStartTimeBetweenOrderByStartTime(7L, from, to);
        verify(payments).findByAppointmentDoctorIdAndPaidAtBetween(7L, from, to);
    }

    @Test
    void doctorRevenueAndPaymentMethodsComeOnlyFromOwnedPayments() {
        when(payments.findByAppointmentDoctorIdAndPaidAtBetween(7L, from, to)).thenReturn(List.of(
                payment("4000.00", PaymentMethod.CASH), payment("5500.00", PaymentMethod.CARD), payment("7000.00", PaymentMethod.QR)));
        var result = service.doctorSummary("doctor", from, to);
        assertEquals(new BigDecimal("16500.00"), result.revenue());
        assertEquals(new BigDecimal("4000.00"), result.payments().cash());
        assertEquals(new BigDecimal("5500.00"), result.payments().card());
        assertEquals(new BigDecimal("7000.00"), result.payments().qr());
    }

    @Test
    void averageCheckDividesRevenueByCompletedAppointments() {
        Patient patient = patient(1L);
        when(appointments.findByDoctorIdAndStartTimeBetweenOrderByStartTime(7L, from, to))
                .thenReturn(List.of(appointment(patient, AppointmentStatus.COMPLETED), appointment(patient, AppointmentStatus.COMPLETED), appointment(patient, AppointmentStatus.COMPLETED)));
        when(payments.findByAppointmentDoctorIdAndPaidAtBetween(7L, from, to))
                .thenReturn(List.of(payment("10000.00", PaymentMethod.CARD)));
        assertEquals(new BigDecimal("3333.33"), service.doctorSummary("doctor", from, to).averageCheck());
    }

    @Test
    void emptyPeriodReturnsSafeZeros() {
        var result = service.doctorSummary("doctor", from, to);
        assertEquals(0, result.patients());
        assertEquals(0, result.completedAppointments());
        assertEquals(new BigDecimal("0.00"), result.averageCheck());
        assertEquals(BigDecimal.ZERO, result.revenue());
    }

    @Test
    void invalidDateRangeIsRejectedBeforeQueries() {
        assertThrows(IllegalArgumentException.class, () -> service.doctorSummary("doctor", to, from));
        verifyNoInteractions(doctors);
    }

    private Patient patient(Long id) { Patient patient = new Patient(); patient.setId(id); return patient; }
    private Appointment appointment(Patient patient, AppointmentStatus status) { Appointment appointment = new Appointment(); appointment.setPatient(patient); appointment.setStatus(status); return appointment; }
    private Payment payment(String amount, PaymentMethod method) { Payment payment = new Payment(); payment.setAmount(new BigDecimal(amount)); payment.setPaymentMethod(method); return payment; }
}
