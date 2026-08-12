package com.dentalcrm.doctor;
import com.dentalcrm.user.UserDtos.CreateDoctorUserRequest; import jakarta.validation.Valid; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.List; import static com.dentalcrm.doctor.DoctorDtos.*;
@RestController @RequestMapping("/api/doctors")
public class DoctorController {private final DoctorService service;public DoctorController(DoctorService s){service=s;}
 @GetMapping public List<DoctorResponse> all(){return service.findAll();}
 @GetMapping("/{id}") public DoctorResponse one(@PathVariable Long id){return service.find(id);}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") public DoctorResponse create(@Valid @RequestBody CreateDoctorUserRequest r){return service.create(r);}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public DoctorResponse update(@PathVariable Long id,@Valid @RequestBody UpdateDoctorRequest r){return service.update(id,r);}
}
