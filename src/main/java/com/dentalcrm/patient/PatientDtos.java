package com.dentalcrm.patient;
import jakarta.validation.constraints.*; import java.time.*;
public final class PatientDtos {private PatientDtos(){}
 public record PatientRequest(
         @NotBlank(message="Укажите ФИО пациента.") @Size(max=200,message="ФИО не должно быть длиннее 200 символов.") String fullName,
         @NotBlank(message="Укажите телефон пациента.") @Size(max=30,message="Телефон не должен быть длиннее 30 символов.") String phone,
         LocalDate birthDate,
         @Size(max=5000,message="Примечания не должны быть длиннее 5000 символов.") String notes){}
 public record PatientResponse(Long id,String fullName,String phone,LocalDate birthDate,String notes,Instant createdAt){}
}
