package com.dentalcrm.service;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ClinicServiceRepository extends JpaRepository<ClinicService,Long>{ List<ClinicService> findByActiveTrueOrderByName(); }
