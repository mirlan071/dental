package com.dentalcrm.auth;
import com.dentalcrm.user.Role;
public record AuthResponse(Long id,String username,String fullName,Role role){}
