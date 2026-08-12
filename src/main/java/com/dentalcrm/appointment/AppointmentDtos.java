package com.dentalcrm.appointment;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*; import java.util.List;
public final class AppointmentDtos {private AppointmentDtos(){}
 public record AppointmentRequest(@NotNull Long patientId,@NotNull Long doctorId,@NotNull @FutureOrPresent OffsetDateTime startTime,@NotNull OffsetDateTime endTime,AppointmentStatus status,@Size(max=5000) String notes,List<@NotNull Long> serviceIds){}
 public record StatusRequest(@NotNull AppointmentStatus status){}
 public record ServiceItemResponse(Long id,Long serviceId,String serviceName,BigDecimal price){}
 public record AppointmentResponse(Long id,Long patientId,String patientName,Long doctorId,String doctorName,OffsetDateTime startTime,OffsetDateTime endTime,AppointmentStatus status,String notes,Instant createdAt,String createdBy,List<ServiceItemResponse> services){}
}
