package com.dentalcrm.appointment;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*; import java.util.List;
public final class AppointmentDtos {private AppointmentDtos(){}
 public record AppointmentRequest(@NotNull Long patientId,@NotNull Long doctorId,@NotNull @FutureOrPresent OffsetDateTime startTime,@NotNull OffsetDateTime endTime,AppointmentStatus status,@Size(max=5000) String notes,List<@NotNull Long> serviceIds,List<@Valid ServiceItemRequest> services){}
 public record ServiceItemRequest(@NotNull Long serviceId,@Min(1) Integer quantity){}
 public record StatusRequest(@NotNull AppointmentStatus status){}
 public record ServiceItemResponse(Long id,Long serviceId,String serviceName,Integer quantity,BigDecimal unitPrice,BigDecimal lineTotal){}
 public record PaymentItemResponse(Long id,BigDecimal amount,String paymentMethod,OffsetDateTime paidAt){}
 public record AppointmentResponse(Long id,Long patientId,String patientName,Long doctorId,String doctorName,OffsetDateTime startTime,OffsetDateTime endTime,AppointmentStatus status,String notes,Instant createdAt,String createdBy,List<ServiceItemResponse> services,BigDecimal servicesTotal,List<PaymentItemResponse> payments,BigDecimal paidTotal,BigDecimal remainingBalance){}
}
