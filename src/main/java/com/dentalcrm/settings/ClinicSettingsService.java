package com.dentalcrm.settings;

import com.dentalcrm.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

import static com.dentalcrm.settings.ClinicSettingsDtos.*;

@Service
@Transactional
public class ClinicSettingsService {
    public static final String CLINIC_TIMEZONE = "Asia/Bishkek";

    private final ClinicSettingsRepository repository;

    public ClinicSettingsService(ClinicSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ClinicSettingsResponse get() {
        return response(entity());
    }

    public ClinicSettingsResponse update(ClinicSettingsRequest request) {
        validateRange(request.workdayStart(), request.workdayEnd());
        ClinicSettings settings = entity();
        settings.setWorkdayStart(request.workdayStart());
        settings.setWorkdayEnd(request.workdayEnd());
        settings.setTimezone(CLINIC_TIMEZONE);
        return response(repository.save(settings));
    }

    @Transactional(readOnly = true)
    public void validateAppointmentTime(OffsetDateTime start, OffsetDateTime end) {
        ClinicSettings settings = entity();
        ZoneId zone = ZoneId.of(settings.getTimezone());
        ZonedDateTime localStart = start.atZoneSameInstant(zone);
        ZonedDateTime localEnd = end.atZoneSameInstant(zone);
        boolean outside = !localStart.toLocalDate().equals(localEnd.toLocalDate())
                || localStart.toLocalTime().isBefore(settings.getWorkdayStart())
                || localEnd.toLocalTime().isAfter(settings.getWorkdayEnd());
        if (outside) {
            throw new IllegalArgumentException("Выбранное время находится вне рабочего времени клиники.");
        }
    }

    private ClinicSettings entity() {
        return repository.findById(ClinicSettings.SINGLETON_ID)
                .orElseThrow(() -> new NotFoundException("Clinic settings are not configured"));
    }

    private void validateRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Начало рабочего дня должно быть раньше окончания.");
        }
    }

    private ClinicSettingsResponse response(ClinicSettings settings) {
        return new ClinicSettingsResponse(settings.getId(), settings.getWorkdayStart(), settings.getWorkdayEnd(),
                settings.getTimezone(), settings.getUpdatedAt());
    }
}
