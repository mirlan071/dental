package com.dentalcrm.doctor;
import com.dentalcrm.user.UserDtos.CreateDoctorUserRequest; import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.List; import static com.dentalcrm.doctor.DoctorDtos.*;
@RestController @RequestMapping("/api/doctors")
public class DoctorController {private final DoctorService service;public DoctorController(DoctorService s){service=s;}
 @GetMapping @PreAuthorize("hasRole('ADMIN')") public List<DoctorResponse> all(){return service.findAll();}
 @GetMapping("/active") @PreAuthorize("isAuthenticated()") public List<ActiveDoctorResponse> activeDoctors(){return service.findActive();}
 @GetMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public DoctorResponse one(@PathVariable Long id){return service.find(id);}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") public DoctorResponse create(@Valid @RequestBody CreateDoctorUserRequest r){return service.create(r);}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public DoctorResponse update(@PathVariable Long id,@Valid @RequestBody UpdateDoctorRequest r){return service.update(id,r);}
 @PatchMapping("/{id}/active") @PreAuthorize("hasRole('ADMIN')") public DoctorResponse active(@PathVariable Long id,@Valid @RequestBody DoctorActiveRequest r){return service.setActive(id,r.active());}
 @PatchMapping("/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void password(@PathVariable Long id,@Valid @RequestBody ResetPasswordRequest r){service.resetPassword(id,r.password());}
}
