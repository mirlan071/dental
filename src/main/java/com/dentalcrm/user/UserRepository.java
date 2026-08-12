package com.dentalcrm.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findFirstByRoleOrderByIdAsc(Role role);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByRole(Role role);
}
