package com.dentalcrm.doctor;
import jakarta.validation.constraints.*;
public final class DoctorDtos {private DoctorDtos(){}
 public record UpdateDoctorRequest(@NotBlank @Size(max=200) String fullName,@Size(max=200) String specialization,@Size(max=30) String phone){}
 public record DoctorActiveRequest(@NotNull Boolean active){}
 public record ResetPasswordRequest(@NotBlank @Size(min=8,max=100) String password){}
 public record DoctorResponse(Long id,Long userId,String username,String fullName,String specialization,String phone,boolean active){}
}
