package com.dentalcrm.service;
import jakarta.validation.Valid; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.List; import static com.dentalcrm.service.ServiceDtos.*;
@RestController @RequestMapping("/api/services") public class ClinicServiceController {private final ClinicServiceManager service;public ClinicServiceController(ClinicServiceManager s){service=s;}
 @GetMapping public List<ServiceResponse> all(@RequestParam(defaultValue="false") boolean activeOnly){return service.all(activeOnly);}
 @GetMapping("/{id}") public ServiceResponse one(@PathVariable Long id){return service.find(id);}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") public ServiceResponse create(@Valid @RequestBody ServiceRequest r){return service.create(r);}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ServiceResponse update(@PathVariable Long id,@Valid @RequestBody ServiceRequest r){return service.update(id,r);}
}
