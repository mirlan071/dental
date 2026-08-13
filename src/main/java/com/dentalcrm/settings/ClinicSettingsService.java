package com.dentalcrm.settings;

import com.dentalcrm.common.NotFoundException;
import com.dentalcrm.common.ConflictException;
import com.dentalcrm.appointment.AppointmentRepository;
import com.dentalcrm.appointment.AppointmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.EnumSet;

import static com.dentalcrm.settings.ClinicSettingsDtos.*;

@Service
@Transactional
public class ClinicSettingsService {
    public static final String CLINIC_TIMEZONE = "Asia/Bishkek";

    private final ClinicSettingsRepository repository;
    private final AppointmentRepository appointments;

    public ClinicSettingsService(ClinicSettingsRepository repository, AppointmentRepository appointments) {
        this.repository = repository;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public ClinicSettingsResponse get() {
        return response(entity());
    }

    public ClinicSettingsResponse update(ClinicSettingsRequest request) {
        validateRange(request.workdayStart(), request.workdayEnd());
        ClinicSettings settings = entityForUpdate();
        boolean narrowsWorkingHours = request.workdayStart().isAfter(settings.getWorkdayStart())
                || request.workdayEnd().isBefore(settings.getWorkdayEnd());
        if (narrowsWorkingHours) {
            ensureFutureAppointmentsFit(request);
        }
        settings.setWorkdayStart(request.workdayStart());
        settings.setWorkdayEnd(request.workdayEnd());
        settings.setTimezone(CLINIC_TIMEZONE);
        repository.saveAndFlush(settings);
        return response(settings);
    }

    @Transactional
    public void validateAppointmentTime(OffsetDateTime start, OffsetDateTime end) {
        ClinicSettings settings = entityForUpdate();
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
                .orElseThrow(() -> new NotFoundException("Настройки клиники не заданы."));
    }

    private ClinicSettings entityForUpdate() {
        return repository.findByIdForUpdate(ClinicSettings.SINGLETON_ID)
                .orElseThrow(() -> new NotFoundException("Настройки клиники не заданы."));
    }

    private void validateRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Начало рабочего дня должно быть раньше окончания.");
        }
        if (start.getSecond() != 0 || start.getNano() != 0 || end.getSecond() != 0 || end.getNano() != 0) {
            throw new IllegalArgumentException("Рабочее время задаётся с точностью до минуты.");
        }
    }

    private void ensureFutureAppointmentsFit(ClinicSettingsRequest request) {
        ZoneId zone = ZoneId.of(CLINIC_TIMEZONE);
        boolean strandsAppointment = appointments.findByStatusInAndEndTimeAfter(
                        EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.IN_PROGRESS,
                                AppointmentStatus.COMPLETED),
                        OffsetDateTime.now(ZoneOffset.UTC))
                .stream()
                .anyMatch(appointment -> {
                    ZonedDateTime start = appointment.getStartTime().atZoneSameInstant(zone);
                    ZonedDateTime end = appointment.getEndTime().atZoneSameInstant(zone);
                    return !start.toLocalDate().equals(end.toLocalDate())
                            || start.toLocalTime().isBefore(request.workdayStart())
                            || end.toLocalTime().isAfter(request.workdayEnd());
                });
        if (strandsAppointment) {
            throw new ConflictException("Новое рабочее время не включает существующие будущие записи.");
        }
    }

    private ClinicSettingsResponse response(ClinicSettings settings) {
        return new ClinicSettingsResponse(settings.getId(), settings.getWorkdayStart(), settings.getWorkdayEnd(),
                settings.getTimezone(), settings.getUpdatedAt());
    }
}
