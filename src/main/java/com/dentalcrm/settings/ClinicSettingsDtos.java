package com.dentalcrm.settings;

import jakarta.validation.constraints.NotNull;

import java.time.*;

public final class ClinicSettingsDtos {
    private ClinicSettingsDtos() {
    }

    public record ClinicSettingsRequest(
            @NotNull(message = "Укажите начало рабочего дня.") LocalTime workdayStart,
            @NotNull(message = "Укажите окончание рабочего дня.") LocalTime workdayEnd
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
