package com.dentalcrm.dashboard;

import com.dentalcrm.appointment.*;
import com.dentalcrm.common.NotFoundException;
import com.dentalcrm.doctor.DoctorRepository;
import com.dentalcrm.patient.PatientRepository;
import com.dentalcrm.payment.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.OffsetDateTime;
import java.util.List;

import static com.dentalcrm.dashboard.DashboardDtos.*;

@Service
public class DashboardService {
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final PaymentRepository payments;
    private final DoctorRepository doctors;

    public DashboardService(PatientRepository patients, AppointmentRepository appointments,
                            PaymentRepository payments, DoctorRepository doctors) {
        this.patients = patients;
        this.appointments = appointments;
        this.payments = payments;
        this.doctors = doctors;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(OffsetDateTime from, OffsetDateTime to) {
        validateRange(from, to);
        var periodAppointments = appointments.findByStartTimeBetweenOrderByStartTime(from, to);
        var periodPayments = payments.findByPaidAtBetween(from, to);
        BigDecimal cash = sum(periodPayments, PaymentMethod.CASH);
        BigDecimal card = sum(periodPayments, PaymentMethod.CARD);
        BigDecimal qr = sum(periodPayments, PaymentMethod.QR);
        return new DashboardSummary(from, to, patients.count(), periodAppointments.size(),
                periodAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count(),
                periodAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count(),
                cash.add(card).add(qr), cash, card, qr);
    }

    @Transactional(readOnly = true)
    public DoctorDashboardSummary doctorSummary(String username, OffsetDateTime from, OffsetDateTime to) {
        validateRange(from, to);
        var doctor = doctors.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));
        List<Appointment> periodAppointments = appointments
                .findByDoctorIdAndStartTimeBetweenOrderByStartTime(doctor.getId(), from, to);
        List<Payment> periodPayments = payments
                .findByAppointmentDoctorIdAndPaidAtBetween(doctor.getId(), from, to);

        long patientCount = periodAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .map(a -> a.getPatient().getId())
                .distinct()
                .count();
        long completedCount = periodAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();
        BigDecimal cash = sum(periodPayments, PaymentMethod.CASH);
        BigDecimal card = sum(periodPayments, PaymentMethod.CARD);
        BigDecimal qr = sum(periodPayments, PaymentMethod.QR);
        BigDecimal revenue = cash.add(card).add(qr);
        BigDecimal averageCheck = completedCount == 0
                ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP);

        return new DoctorDashboardSummary(from, to, patientCount, completedCount, revenue, averageCheck,
                new DoctorPaymentBreakdown(cash, card, qr));
    }

    private BigDecimal sum(List<Payment> source, PaymentMethod method) {
        return source.stream().filter(payment -> payment.getPaymentMethod() == method)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (!to.isAfter(from)) throw new IllegalArgumentException("to must be after from");
    }
}
