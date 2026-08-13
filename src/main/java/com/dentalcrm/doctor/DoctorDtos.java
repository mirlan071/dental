package com.dentalcrm.doctor;
import jakarta.validation.constraints.*;
public final class DoctorDtos {private DoctorDtos(){}
 public record UpdateDoctorRequest(
         @NotBlank(message="Укажите ФИО врача.") @Size(max=200,message="ФИО не должно быть длиннее 200 символов.") String fullName,
         @Size(max=200,message="Специализация не должна быть длиннее 200 символов.") String specialization,
         @Size(max=30,message="Телефон не должен быть длиннее 30 символов.") String phone){}
 public record DoctorActiveRequest(@NotNull(message="Укажите статус врача.") Boolean active){}
 public record ResetPasswordRequest(
         @NotBlank(message="Укажите новый пароль.") @Size(min=8,max=100,message="Пароль должен содержать от 8 до 100 символов.") String password){}
 public record ActiveDoctorResponse(Long id,String fullName,String specialization){}
 public record DoctorResponse(Long id,Long userId,String username,String fullName,String specialization,String phone,boolean active){}
}
