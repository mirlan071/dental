package com.dentalcrm.service;
import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface ClinicServiceRepository extends JpaRepository<ClinicService,Long>{ List<ClinicService> findByActiveTrueOrderByName(); Optional<ClinicService> findByName(String name); @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select service from ClinicService service where service.id=:id") Optional<ClinicService> findByIdForUpdate(@Param("id") Long id); }
