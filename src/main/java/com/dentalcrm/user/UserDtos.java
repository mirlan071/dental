package com.dentalcrm.user;
import jakarta.validation.constraints.*; import java.time.Instant;
public final class UserDtos {private UserDtos(){}
 public record CreateDoctorUserRequest(
         @NotBlank(message="Укажите имя пользователя.") @Size(max=100,message="Имя пользователя не должно быть длиннее 100 символов.") String username,
         @NotBlank(message="Укажите пароль.") @Size(min=8,max=100,message="Пароль должен содержать от 8 до 100 символов.") String password,
         @NotBlank(message="Укажите ФИО врача.") @Size(max=200,message="ФИО не должно быть длиннее 200 символов.") String fullName,
         @Size(max=200,message="Специализация не должна быть длиннее 200 символов.") String specialization,
         @Size(max=30,message="Телефон не должен быть длиннее 30 символов.") String phone){}
 public record UserResponse(Long id,String username,String fullName,Role role,boolean active,Instant createdAt){}
}
