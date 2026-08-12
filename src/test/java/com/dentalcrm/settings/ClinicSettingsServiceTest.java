package com.dentalcrm.settings;

import org.junit.jupiter.api.*;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClinicSettingsServiceTest {
    private ClinicSettingsRepository repository;
    private ClinicSettingsService service;
    private ClinicSettings settings;

    @BeforeEach
    void setUp() {
        repository = mock(ClinicSettingsRepository.class);
        service = new ClinicSettingsService(repository);
        settings = new ClinicSettings();
        settings.setId(ClinicSettings.SINGLETON_ID);
        settings.setWorkdayStart(LocalTime.of(9, 0));
        settings.setWorkdayEnd(LocalTime.of(18, 0));
        settings.setTimezone(ClinicSettingsService.CLINIC_TIMEZONE);
        when(repository.findById(ClinicSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updatesValidWorkingHours() {
        var response = service.update(new ClinicSettingsDtos.ClinicSettingsRequest(
                LocalTime.of(8, 0), LocalTime.of(20, 0)));
        assertEquals(LocalTime.of(8, 0), response.workdayStart());
        assertEquals(LocalTime.of(20, 0), response.workdayEnd());
        assertEquals("Asia/Bishkek", response.timezone());
    }

    @Test
    void rejectsInvalidWorkingHours() {
        assertThrows(IllegalArgumentException.class, () -> service.update(
                new ClinicSettingsDtos.ClinicSettingsRequest(LocalTime.of(18, 0), LocalTime.of(9, 0))));
    }

    @Test
    void acceptsAppointmentExactlyInsideBoundaries() {
        assertDoesNotThrow(() -> service.validateAppointmentTime(
                OffsetDateTime.parse("2026-08-12T09:00:00+06:00"),
                OffsetDateTime.parse("2026-08-12T18:00:00+06:00")));
    }

    @Test
    void rejectsAppointmentBeforeOpening() {
        assertThrows(IllegalArgumentException.class, () -> service.validateAppointmentTime(
                OffsetDateTime.parse("2026-08-12T08:30:00+06:00"),
                OffsetDateTime.parse("2026-08-12T09:30:00+06:00")));
    }

    @Test
    void rejectsAppointmentEndingAfterClosing() {
        assertThrows(IllegalArgumentException.class, () -> service.validateAppointmentTime(
                OffsetDateTime.parse("2026-08-12T17:30:00+06:00"),
                OffsetDateTime.parse("2026-08-12T18:30:00+06:00")));
    }
}
