package com.dentalcrm.payment;
import jakarta.validation.Valid; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.List; import static com.dentalcrm.payment.PaymentDtos.*;
@RestController @RequestMapping("/api/payments") public class PaymentController {private final PaymentService service;public PaymentController(PaymentService s){service=s;}
 @PostMapping public PaymentResponse create(@Valid @RequestBody PaymentRequest r,Authentication a){return service.create(r,a);}
 @GetMapping("/{id}") public PaymentResponse one(@PathVariable Long id,Authentication a){return service.find(id,a);}
 @GetMapping public List<PaymentResponse> forAppointment(@RequestParam Long appointmentId,Authentication a){return service.forAppointment(appointmentId,a);}
}
