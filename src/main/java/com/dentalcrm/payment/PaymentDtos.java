package com.dentalcrm.payment;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*;
public final class PaymentDtos {private PaymentDtos(){}
 public record PaymentRequest(@NotNull Long appointmentId,@NotNull @DecimalMin(value="0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@NotNull PaymentMethod paymentMethod,@NotNull OffsetDateTime paidAt){}
 public record PaymentResponse(Long id,Long appointmentId,BigDecimal amount,PaymentMethod paymentMethod,OffsetDateTime paidAt,Instant createdAt){}
}
