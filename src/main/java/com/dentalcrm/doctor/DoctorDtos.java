package com.dentalcrm.doctor;
import jakarta.validation.constraints.Size;
public final class DoctorDtos {private DoctorDtos(){}
 public record UpdateDoctorRequest(@Size(max=200) String specialization,@Size(max=30) String phone,Boolean active){}
 public record DoctorResponse(Long id,Long userId,String username,String fullName,String specialization,String phone,boolean active){}
}
