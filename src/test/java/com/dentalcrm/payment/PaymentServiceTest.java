package com.dentalcrm.payment;

import com.dentalcrm.appointment.*;
import com.dentalcrm.common.ConflictException;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private PaymentRepository payments;
    private AppointmentRepository appointments;
    private AppointmentManager appointmentManager;
    private PaymentService service;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        appointments = mock(AppointmentRepository.class);
        appointmentManager = mock(AppointmentManager.class);
        service = new PaymentService(payments, appointments, appointmentManager);
        appointment = new Appointment();
        appointment.setId(7L);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointments.findByIdForUpdate(7L)).thenReturn(Optional.of(appointment));
        when(appointmentManager.servicesTotal(7L)).thenReturn(new BigDecimal("4000.00"));
        when(payments.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(100L);
            return payment;
        });
    }

    @Test
    void acceptsPartialCashPayment() {
        when(appointmentManager.paidTotal(7L)).thenReturn(BigDecimal.ZERO);
        var response = service.create(request("1000.00", PaymentMethod.CASH), auth());
        assertEquals(PaymentMethod.CASH, response.paymentMethod());
        assertEquals(new BigDecimal("1000.00"), response.amount());
    }

    @Test
    void acceptsSplitCardPaymentUpToRemainingBalance() {
        when(appointmentManager.paidTotal(7L)).thenReturn(new BigDecimal("1000.00"));
        var response = service.create(request("2000.00", PaymentMethod.CARD), auth());
        assertEquals(PaymentMethod.CARD, response.paymentMethod());
    }

    @Test
    void acceptsQrPaymentForExactRemainingBalance() {
        when(appointmentManager.paidTotal(7L)).thenReturn(new BigDecimal("3000.00"));
        var response = service.create(request("1000.00", PaymentMethod.QR), auth());
        assertEquals(PaymentMethod.QR, response.paymentMethod());
    }

    @Test
    void rejectsOverpayment() {
        when(appointmentManager.paidTotal(7L)).thenReturn(new BigDecimal("3000.00"));
        ConflictException error = assertThrows(ConflictException.class,
                () -> service.create(request("1500.00", PaymentMethod.CASH), auth()));
        assertTrue(error.getMessage().contains("остаток 1000.00"));
        verify(payments, never()).save(any());
    }

    @Test
    void rejectsPaymentBeforeAppointmentIsCompleted() {
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        assertThrows(ConflictException.class,
                () -> service.create(request("1000.00", PaymentMethod.CASH), auth()));
        verify(payments, never()).save(any());
    }

    @Test
    void rejectsFuturePaymentDate() {
        var future = new PaymentDtos.PaymentRequest(7L, new BigDecimal("1000.00"), PaymentMethod.CASH,
                OffsetDateTime.now().plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.create(future, auth()));
        verify(payments, never()).save(any());
    }

    private PaymentDtos.PaymentRequest request(String amount, PaymentMethod method) {
        return new PaymentDtos.PaymentRequest(7L, new BigDecimal(amount), method, OffsetDateTime.now());
    }

    private UsernamePasswordAuthenticationToken auth() {
        return UsernamePasswordAuthenticationToken.authenticated("admin", "", java.util.List.of());
    }
}
