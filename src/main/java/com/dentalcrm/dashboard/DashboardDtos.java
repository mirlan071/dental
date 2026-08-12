package com.dentalcrm.dashboard;

import com.dentalcrm.appointment.AppointmentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardSummary(
            OffsetDateTime from,
            OffsetDateTime to,
            long totalPatients,
            long appointments,
            long completedAppointments,
            long cancelledAppointments,
            BigDecimal clinicRevenue,
            BigDecimal cashRevenue,
            BigDecimal cardRevenue,
            BigDecimal qrRevenue,
            BigDecimal servicesPerformed,
            BigDecimal paymentsReceived,
            BigDecimal outstandingAmount,
            long debtorCount,
            BigDecimal totalDebt,
            List<DoctorPerformance> doctors
    ) {}

    public record DoctorPerformance(Long doctorId, String doctorFullName, long patients,
                                    long completedAppointments, BigDecimal servicesPerformed,
                                    BigDecimal paymentsReceived, BigDecimal outstandingAmount) {}

    public record DebtorDoctor(Long id, String fullName) {}

    public record DebtorSummary(Long patientId, String patientFullName, String phone,
                                BigDecimal totalTreatmentAmount, BigDecimal totalPaid,
                                BigDecimal totalDebt, OffsetDateTime lastTreatmentDate,
                                long unpaidAppointments, List<DebtorDoctor> doctors) {}

    public record DebtAppointment(Long appointmentId, OffsetDateTime date, Long doctorId,
                                  String doctorName, List<String> services, BigDecimal appointmentTotal,
                                  BigDecimal paid, BigDecimal remainingBalance, AppointmentStatus status) {}

    public record PatientDebtDetails(Long patientId, String patientFullName, String phone,
                                     BigDecimal totalTreatmentAmount, BigDecimal totalPaid,
                                     BigDecimal totalDebt, List<DebtAppointment> appointments) {}

    public record DoctorPaymentBreakdown(BigDecimal cash, BigDecimal card, BigDecimal qr) {}

    public record DoctorDashboardSummary(
            OffsetDateTime from,
            OffsetDateTime to,
            long patients,
            long completedAppointments,
            BigDecimal revenue,
            BigDecimal averageCheck,
            DoctorPaymentBreakdown payments
    ) {}
}
