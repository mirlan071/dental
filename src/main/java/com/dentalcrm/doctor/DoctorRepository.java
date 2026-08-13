package com.dentalcrm.doctor;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface DoctorRepository extends JpaRepository<Doctor,Long>{ Optional<Doctor> findByUserUsername(String username); List<Doctor> findByUserActiveTrueOrderByUserFullName(); List<Doctor> findAllByOrderByUserFullName(); }
