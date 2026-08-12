package com.dentalcrm.patient;
import jakarta.validation.constraints.*; import java.time.*;
public final class PatientDtos {private PatientDtos(){}
 public record PatientRequest(@NotBlank @Size(max=200) String fullName,@NotBlank @Size(max=30) String phone,@Past LocalDate birthDate,@Size(max=5000) String notes){}
 public record PatientResponse(Long id,String fullName,String phone,LocalDate birthDate,String notes,Instant createdAt){}
}
