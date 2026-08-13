package com.dentalcrm.service;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public final class ServiceDtos {private ServiceDtos(){}
 public record ServiceRequest(
         @NotBlank(message="Укажите название услуги.") @Size(max=200,message="Название не должно быть длиннее 200 символов.") String name,
         @NotNull(message="Укажите цену услуги.") @DecimalMin(value="0.00",message="Цена не может быть отрицательной.") @Digits(integer=10,fraction=2,message="Цена должна содержать не более 10 целых знаков и 2 знаков после запятой.") BigDecimal price,
         @NotNull(message="Укажите длительность услуги.") @Min(value=1,message="Длительность должна быть не меньше 1 минуты.") @Max(value=1440,message="Длительность не должна превышать 1440 минут.") Integer durationMinutes,
         Boolean active){}
 public record ServiceResponse(Long id,String name,BigDecimal price,Integer durationMinutes,boolean active){}
}
