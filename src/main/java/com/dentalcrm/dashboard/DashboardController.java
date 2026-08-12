package com.dentalcrm.dashboard;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

import static com.dentalcrm.dashboard.DashboardDtos.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardSummary summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return service.summary(from, to);
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorDashboardSummary doctorSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Authentication authentication) {
        return service.doctorSummary(authentication.getName(), from, to);
    }

    @GetMapping("/debtors")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DebtorSummary> debtors(@RequestParam(required = false) String search) {
        return service.debtors(search);
    }

    @GetMapping("/debtors/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public PatientDebtDetails debtor(@PathVariable Long patientId) {
        return service.patientDebt(patientId);
    }
}
