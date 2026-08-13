package com.dentalcrm.settings;

import com.dentalcrm.appointment.*;
import com.dentalcrm.common.ConflictException;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.Lock;

import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClinicSettingsServiceTest {
    private ClinicSettingsRepository repository;
    private ClinicSettingsService service;
    private ClinicSettings settings;
    private AppointmentRepository appointments;

    @BeforeEach
    void setUp() {
        repository = mock(ClinicSettingsRepository.class);
        appointments = mock(AppointmentRepository.class);
        service = new ClinicSettingsService(repository, appointments);
        settings = new ClinicSettings();
        settings.setId(ClinicSettings.SINGLETON_ID);
        settings.setWorkdayStart(LocalTime.of(9, 0));
        settings.setWorkdayEnd(LocalTime.of(18, 0));
        settings.setTimezone(ClinicSettingsService.CLINIC_TIMEZONE);
        when(repository.findById(ClinicSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
        when(repository.findByIdForUpdate(ClinicSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            ClinicSettings value = invocation.getArgument(0);
            value.updateTimestamp();
            return value;
        });
    }

    @Test
    void updatesValidWorkingHours() {
        var response = service.update(new ClinicSettingsDtos.ClinicSettingsRequest(
                LocalTime.of(8, 0), LocalTime.of(20, 0)));
        assertEquals(LocalTime.of(8, 0), response.workdayStart());
        assertEquals(LocalTime.of(20, 0), response.workdayEnd());
        assertEquals("Asia/Bishkek", response.timezone());
        verify(repository).findByIdForUpdate(ClinicSettings.SINGLETON_ID);
    }

    @Test
    void rejectsInvalidWorkingHours() {
        assertThrows(IllegalArgumentException.class, () -> service.update(
                new ClinicSettingsDtos.ClinicSettingsRequest(LocalTime.of(18, 0), LocalTime.of(9, 0))));
    }

    @Test
    void rejectsWorkingHoursWithSeconds() {
        assertThrows(IllegalArgumentException.class, () -> service.update(
                new ClinicSettingsDtos.ClinicSettingsRequest(LocalTime.of(9, 0, 30), LocalTime.of(18, 0))));
    }

    @Test
    void rejectsNarrowingThatWouldStrandFutureAppointment() {
        Appointment appointment = new Appointment();
        appointment.setStartTime(OffsetDateTime.now(ZoneId.of("Asia/Bishkek")).plusDays(1).withHour(17).withMinute(0));
        appointment.setEndTime(appointment.getStartTime().plusHours(1));
        when(appointments.findByStatusInAndEndTimeAfter(anyCollection(), any())).thenReturn(List.of(appointment));

        assertThrows(ConflictException.class, () -> service.update(
                new ClinicSettingsDtos.ClinicSettingsRequest(LocalTime.of(9, 0), LocalTime.of(17, 30))));
        verify(repository, never()).save(any());
        verify(appointments).findByStatusInAndEndTimeAfter(
                eq(EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.IN_PROGRESS,
                        AppointmentStatus.COMPLETED)), any());
    }

    @Test
    void acceptsNarrowingWhenFutureAppointmentFitsExactBoundaries() {
        Appointment appointment = new Appointment();
        appointment.setStartTime(OffsetDateTime.parse("2030-01-01T10:00:00+06:00"));
        appointment.setEndTime(OffsetDateTime.parse("2030-01-01T17:00:00+06:00"));
        when(appointments.findByStatusInAndEndTimeAfter(anyCollection(), any())).thenReturn(List.of(appointment));

        var response = service.update(new ClinicSettingsDtos.ClinicSettingsRequest(
                LocalTime.of(10, 0), LocalTime.of(17, 0)));

        assertEquals(LocalTime.of(10, 0), response.workdayStart());
        assertEquals(LocalTime.of(17, 0), response.workdayEnd());
    }

    @Test
    void wideningWorkingHoursDoesNotInspectAppointments() {
        service.update(new ClinicSettingsDtos.ClinicSettingsRequest(
                LocalTime.of(8, 0), LocalTime.of(19, 0)));

        verifyNoInteractions(appointments);
    }

    @Test
    void settingsUpdateLookupUsesPessimisticWriteLock() throws Exception {
        Lock lock = ClinicSettingsRepository.class
                .getMethod("findByIdForUpdate", Long.class)
                .getAnnotation(Lock.class);

        assertNotNull(lock);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    @Test
    void acceptsAppointmentExactlyInsideBoundaries() {
        assertDoesNotThrow(() -> service.validateAppointmentTime(
                OffsetDateTime.parse("2026-08-12T09:00:00+06:00"),
                OffsetDateTime.parse("2026-08-12T18:00:00+06:00")));
        verify(repository).findByIdForUpdate(ClinicSettings.SINGLETON_ID);
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
