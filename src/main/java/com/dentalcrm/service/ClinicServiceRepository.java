package com.dentalcrm.service;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ClinicServiceRepository extends JpaRepository<ClinicService,Long>{ List<ClinicService> findByActiveTrueOrderByName(); Optional<ClinicService> findByName(String name); }
