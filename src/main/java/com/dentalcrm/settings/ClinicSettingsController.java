package com.dentalcrm.settings;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.dentalcrm.settings.ClinicSettingsDtos.*;

@RestController
@RequestMapping("/api/settings/clinic")
public class ClinicSettingsController {
    private final ClinicSettingsService service;

    public ClinicSettingsController(ClinicSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ClinicSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ClinicSettingsResponse update(@Valid @RequestBody ClinicSettingsRequest request) {
        return service.update(request);
    }
}
