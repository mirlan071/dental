package com.dentalcrm.patient;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface PatientRepository extends JpaRepository<Patient,Long>{ List<Patient> findByFullNameContainingIgnoreCaseOrPhoneContaining(String name,String phone); Optional<Patient> findByPhone(String phone); boolean existsByPhone(String phone); }
