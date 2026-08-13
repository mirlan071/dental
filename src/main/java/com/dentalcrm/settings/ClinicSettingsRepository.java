package com.dentalcrm.settings;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClinicSettingsRepository extends JpaRepository<ClinicSettings, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select settings from ClinicSettings settings where settings.id = :id")
    Optional<ClinicSettings> findByIdForUpdate(@Param("id") Long id);
}
