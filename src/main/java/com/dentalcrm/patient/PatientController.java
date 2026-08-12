package com.dentalcrm.patient;
import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import java.util.List; import static com.dentalcrm.patient.PatientDtos.*;
@RestController @RequestMapping("/api/patients") public class PatientController {private final PatientService service;public PatientController(PatientService s){service=s;}
 @GetMapping public List<PatientResponse> all(@RequestParam(required=false) String search){return service.all(search);}
 @GetMapping("/{id}") public PatientResponse one(@PathVariable Long id){return service.find(id);}
 @PostMapping public PatientResponse create(@Valid @RequestBody PatientRequest r){return service.create(r);}
 @PutMapping("/{id}") public PatientResponse update(@PathVariable Long id,@Valid @RequestBody PatientRequest r){return service.update(id,r);}
}
