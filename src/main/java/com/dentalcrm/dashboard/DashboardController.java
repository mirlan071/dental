package com.dentalcrm.dashboard;
import org.springframework.format.annotation.DateTimeFormat; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.time.OffsetDateTime; import static com.dentalcrm.dashboard.DashboardDtos.*;
@RestController @RequestMapping("/api/dashboard") @PreAuthorize("hasRole('ADMIN')") public class DashboardController {private final DashboardService service;public DashboardController(DashboardService s){service=s;}
 @GetMapping("/summary") public DashboardSummary summary(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to){return service.summary(from,to);}
}
