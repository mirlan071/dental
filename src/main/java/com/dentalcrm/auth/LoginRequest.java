package com.dentalcrm.auth;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(
        @NotBlank(message = "Укажите имя пользователя.") String username,
        @NotBlank(message = "Укажите пароль.") String password) {}
