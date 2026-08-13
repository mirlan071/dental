package com.dentalcrm.payment;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*;
public final class PaymentDtos {private PaymentDtos(){}
 public record PaymentRequest(
         @NotNull(message="Укажите приём.") Long appointmentId,
         @NotNull(message="Укажите сумму оплаты.") @DecimalMin(value="0.01",message="Сумма оплаты должна быть не меньше 0,01 сом.") @Digits(integer=10,fraction=2,message="Сумма оплаты должна содержать не более 10 целых знаков и 2 знаков после запятой.") BigDecimal amount,
         @NotNull(message="Укажите способ оплаты.") PaymentMethod paymentMethod,
         @NotNull(message="Укажите дату оплаты.") @PastOrPresent(message="Дата оплаты не может быть в будущем.") OffsetDateTime paidAt){}
 public record PaymentResponse(Long id,Long appointmentId,BigDecimal amount,PaymentMethod paymentMethod,OffsetDateTime paidAt,Instant createdAt){}
}
