package com.dentalcrm.dashboard;
import java.math.BigDecimal; import java.time.OffsetDateTime;
public final class DashboardDtos {private DashboardDtos(){}
 public record DashboardSummary(OffsetDateTime from,OffsetDateTime to,long totalPatients,long appointments,long completedAppointments,long cancelledAppointments,BigDecimal clinicRevenue,BigDecimal cashRevenue,BigDecimal cardRevenue,BigDecimal qrRevenue){}
}
