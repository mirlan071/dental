package com.dentalcrm.user;
import jakarta.validation.constraints.*; import java.time.Instant;
public final class UserDtos {private UserDtos(){}
 public record CreateDoctorUserRequest(@NotBlank @Size(max=100) String username,@NotBlank @Size(min=8,max=100) String password,@NotBlank @Size(max=200) String fullName,@Size(max=200) String specialization,@Size(max=30) String phone){}
 public record UserResponse(Long id,String username,String fullName,Role role,boolean active,Instant createdAt){}
}
