package com.dentalcrm.appointment;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*; import java.util.List;
public final class AppointmentDtos {private AppointmentDtos(){}
 public record AppointmentRequest(
         @NotNull(message="Укажите пациента.") Long patientId,
         @NotNull(message="Укажите врача.") Long doctorId,
         @NotNull(message="Укажите время начала.") OffsetDateTime startTime,
         @NotNull(message="Укажите время окончания.") OffsetDateTime endTime,
         AppointmentStatus status,
         @Size(max=5000,message="Примечания не должны быть длиннее 5000 символов.") String notes,
         List<@NotNull(message="Укажите услугу.") Long> serviceIds,
         List<@NotNull(message="Укажите услугу.") @Valid ServiceItemRequest> services){}
 public record ServiceItemRequest(
         @NotNull(message="Укажите услугу.") Long serviceId,
         @Min(value=1,message="Количество услуги должно быть не меньше 1.") Integer quantity){}
 public record StatusRequest(@NotNull(message="Укажите статус приёма.") AppointmentStatus status){}
 public record ServiceItemResponse(Long id,Long serviceId,String serviceName,Integer quantity,BigDecimal unitPrice,BigDecimal lineTotal){}
 public record PaymentItemResponse(Long id,BigDecimal amount,String paymentMethod,OffsetDateTime paidAt){}
 public record AppointmentResponse(Long id,Long patientId,String patientName,Long doctorId,String doctorName,OffsetDateTime startTime,OffsetDateTime endTime,AppointmentStatus status,String notes,Instant createdAt,String createdBy,List<ServiceItemResponse> services,BigDecimal servicesTotal,List<PaymentItemResponse> payments,BigDecimal paidTotal,BigDecimal remainingBalance){}
}
