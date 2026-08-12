package com.dentalcrm.settings;

import jakarta.validation.constraints.NotNull;

import java.time.*;

public final class ClinicSettingsDtos {
    private ClinicSettingsDtos() {
    }

    public record ClinicSettingsRequest(
            @NotNull LocalTime workdayStart,
            @NotNull LocalTime workdayEnd
    ) {
    }

    public record ClinicSettingsResponse(
            Long id,
            LocalTime workdayStart,
            LocalTime workdayEnd,
            String timezone,
            Instant updatedAt
    ) {
    }
}
