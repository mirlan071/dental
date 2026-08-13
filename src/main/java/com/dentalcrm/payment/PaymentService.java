package com.dentalcrm.payment;

import com.dentalcrm.appointment.*;
import com.dentalcrm.common.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.dentalcrm.payment.PaymentDtos.*;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository repo;
    private final AppointmentRepository appointments;
    private final AppointmentManager appointmentManager;

    public PaymentService(PaymentRepository repo, AppointmentRepository appointments, AppointmentManager appointmentManager) {
        this.repo = repo;
        this.appointments = appointments;
        this.appointmentManager = appointmentManager;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse create(PaymentRequest request, Authentication auth) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма оплаты должна быть больше нуля.");
        }
        if (request.paidAt() == null || request.paidAt().toInstant().isAfter(java.time.Instant.now())) {
            throw new IllegalArgumentException("Дата оплаты не может быть в будущем.");
        }
        Appointment appointment = appointments.findByIdForUpdate(request.appointmentId())
                .orElseThrow(() -> new NotFoundException("Приём не найден: " + request.appointmentId()));
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException("Оплату можно добавить только к завершённому приёму.");
        }
        BigDecimal servicesTotal = appointmentManager.servicesTotal(appointment.getId());
        BigDecimal paidTotal = appointmentManager.paidTotal(appointment.getId());
        BigDecimal remaining = servicesTotal.subtract(paidTotal);
        if (request.amount().compareTo(remaining) > 0) {
            throw new ConflictException("Оплата превышает остаток " + remaining + " сом.");
        }
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaidAt(request.paidAt());
        return map(repo.save(payment));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> forAppointment(Long appointmentId, Authentication auth) {
        appointmentManager.find(appointmentId, auth);
        return repo.findByAppointmentIdOrderByPaidAt(appointmentId).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse find(Long id, Authentication auth) {
        Payment payment = repo.findById(id).orElseThrow(() -> new NotFoundException("Платёж не найден: " + id));
        appointmentManager.find(payment.getAppointment().getId(), auth);
        return map(payment);
    }

    private PaymentResponse map(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getAppointment().getId(), payment.getAmount(),
                payment.getPaymentMethod(), payment.getPaidAt(), payment.getCreatedAt());
    }
}
