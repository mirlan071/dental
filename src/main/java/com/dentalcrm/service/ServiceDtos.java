package com.dentalcrm.service;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public final class ServiceDtos {private ServiceDtos(){}
 public record ServiceRequest(@NotBlank @Size(max=200) String name,@NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal price,@NotNull @Min(1) @Max(1440) Integer durationMinutes,Boolean active){}
 public record ServiceResponse(Long id,String name,BigDecimal price,Integer durationMinutes,boolean active){}
}
