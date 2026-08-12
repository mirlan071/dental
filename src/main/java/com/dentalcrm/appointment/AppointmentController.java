package com.dentalcrm.appointment;
import jakarta.validation.Valid; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.time.*; import java.util.List; import static com.dentalcrm.appointment.AppointmentDtos.*;
@RestController @RequestMapping("/api/appointments") public class AppointmentController {private final AppointmentManager service;public AppointmentController(AppointmentManager s){service=s;}
 @GetMapping public List<AppointmentResponse> all(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,Authentication a){return service.list(from,to,a);}
 @GetMapping("/{id}") public AppointmentResponse one(@PathVariable Long id,Authentication a){return service.find(id,a);}
 @PostMapping public AppointmentResponse create(@Valid @RequestBody AppointmentRequest r,Authentication a){return service.create(r,a);}
 @PutMapping("/{id}") public AppointmentResponse update(@PathVariable Long id,@Valid @RequestBody AppointmentRequest r,Authentication a){return service.update(id,r,a);}
 @PatchMapping("/{id}/status") public AppointmentResponse status(@PathVariable Long id,@Valid @RequestBody StatusRequest r,Authentication a){return service.status(id,r.status(),a);}
}
