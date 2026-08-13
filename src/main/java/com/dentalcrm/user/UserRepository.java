package com.dentalcrm.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findFirstByRoleOrderByIdAsc(Role role);
    Optional<User> findFirstByRoleAndActiveTrueOrderByIdAsc(Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByRole(Role role);
    boolean existsByRoleAndActiveTrue(Role role);
}
