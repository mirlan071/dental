package com.dentalcrm.dashboard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
            BigDecimal qrRevenue
    ) {}

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
