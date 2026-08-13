package com.dentalcrm.appointment;

import com.dentalcrm.common.ConflictException;
import com.dentalcrm.doctor.*;
import com.dentalcrm.patient.*;
import com.dentalcrm.payment.*;
import com.dentalcrm.service.*;
import com.dentalcrm.settings.ClinicSettingsService;
import com.dentalcrm.user.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentWorkflowTest {
    private AppointmentRepository appointments;
    private AppointmentServiceItemRepository items;
    private PaymentRepository payments;
    private DoctorRepository doctorRepository;
    private AppointmentManager manager;
    private Appointment appointment;
    private ClinicSettingsService clinicSettings;

    @BeforeEach
    void setUp() {
        appointments = mock(AppointmentRepository.class);
        items = mock(AppointmentServiceItemRepository.class);
        payments = mock(PaymentRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        clinicSettings = mock(ClinicSettingsService.class);
        manager = new AppointmentManager(appointments, items, payments, mock(PatientService.class),
                mock(DoctorService.class), mock(ClinicServiceManager.class), mock(UserRepository.class), doctorRepository,
                clinicSettings);
        appointment = appointment(10L, 20L, AppointmentStatus.SCHEDULED);
        when(appointments.findById(10L)).thenReturn(Optional.of(appointment));
        when(appointments.findByIdForUpdate(10L)).thenReturn(Optional.of(appointment));
        when(items.findByAppointmentId(anyLong())).thenReturn(List.of());
        when(payments.findByAppointmentIdOrderByPaidAt(anyLong())).thenReturn(List.of());
    }

    @Test
    void quantityCalculatesLineTotal() {
        AppointmentServiceItem item = serviceItem("1250.00", 3);
        assertEquals(new BigDecimal("3750.00"), item.lineTotal());
    }

    @Test
    void quantityDefaultsToOne() {
        AppointmentServiceItem item = new AppointmentServiceItem();
        item.setPrice(new BigDecimal("1250.00"));
        assertEquals(1, item.getQuantity());
        assertEquals(new BigDecimal("1250.00"), item.lineTotal());
    }

    @Test
    void servicesTotalSumsPriceTimesQuantity() {
        when(items.findByAppointmentId(10L)).thenReturn(List.of(serviceItem("1000.00", 2), serviceItem("500.00", 3)));
        assertEquals(new BigDecimal("3500.00"), manager.servicesTotal(10L));
    }

    @Test
    void allowsScheduledToInProgress() {
        var response = manager.status(10L, AppointmentStatus.IN_PROGRESS, admin());
        assertEquals(AppointmentStatus.IN_PROGRESS, response.status());
    }

    @Test
    void allowsInProgressToCompletedWhenServicesExist() {
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        when(items.existsByAppointmentId(10L)).thenReturn(true);
        var response = manager.status(10L, AppointmentStatus.COMPLETED, admin());
        assertEquals(AppointmentStatus.COMPLETED, response.status());
    }

    @Test
    void rejectsInvalidTerminalStatusTransition() {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        assertThrows(ConflictException.class,
                () -> manager.status(10L, AppointmentStatus.IN_PROGRESS, admin()));
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    void rejectsCompletingAppointmentWithoutServices() {
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        when(items.existsByAppointmentId(10L)).thenReturn(false);
        ConflictException error = assertThrows(ConflictException.class,
                () -> manager.status(10L, AppointmentStatus.COMPLETED, admin()));
        assertTrue(error.getMessage().contains("без услуг"));
    }

    @Test
    void doctorCannotAccessAnotherDoctorsAppointment() {
        User doctorUser = appointment(99L, 99L, AppointmentStatus.SCHEDULED).getDoctor().getUser();
        Doctor ownDoctor = new Doctor();
        ownDoctor.setId(99L);
        ownDoctor.setUser(doctorUser);
        when(doctorRepository.findByUserUsername("doctor")).thenReturn(Optional.of(ownDoctor));
        assertThrows(AccessDeniedException.class, () -> manager.find(10L, doctor()));
    }

    @Test
    void doctorCanTransitionOwnAppointment() {
        when(doctorRepository.findByUserUsername("doctor")).thenReturn(Optional.of(appointment.getDoctor()));
        assertEquals(AppointmentStatus.IN_PROGRESS,
                manager.status(10L, AppointmentStatus.IN_PROGRESS, doctor()).status());
    }

    @Test
    void doctorWithoutProfileCannotSeeAllAppointments() {
        when(doctorRepository.findByUserUsername("doctor")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> manager.list(
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1), doctor()));
        verify(appointments, never()).findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(any(), any());
    }

    @Test
    void terminalAppointmentCannotBeEdited() {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        assertThrows(ConflictException.class, () -> manager.update(10L, request(20L, List.of()), admin()));
        verify(items, never()).deleteAll(any());
    }

    @Test
    void updateRejectsReassignmentToInactiveDoctor() {
        DoctorService doctorService = mock(DoctorService.class);
        AppointmentManager updateManager = new AppointmentManager(appointments, items, payments, mock(PatientService.class),
                doctorService, mock(ClinicServiceManager.class), mock(UserRepository.class), doctorRepository, clinicSettings);
        Doctor inactive = appointment(11L, 21L, AppointmentStatus.SCHEDULED).getDoctor();
        inactive.getUser().setActive(false);
        when(doctorService.getForAssignment(21L)).thenReturn(inactive);
        assertThrows(IllegalArgumentException.class, () -> updateManager.update(10L,
                request(21L, List.of(new AppointmentDtos.ServiceItemRequest(1L, 1))), admin()));
        verify(doctorService).getForAssignment(21L);
    }

    @Test
    void updateKeepsHistoricalPriceForUnchangedService() {
        PatientService patientService = mock(PatientService.class);
        DoctorService doctorService = mock(DoctorService.class);
        ClinicServiceManager serviceManager = mock(ClinicServiceManager.class);
        AppointmentManager updateManager = new AppointmentManager(appointments, items, payments, patientService,
                doctorService, serviceManager, mock(UserRepository.class), doctorRepository, clinicSettings);
        ClinicService clinicService = new ClinicService();
        clinicService.setId(5L);
        clinicService.setName("Лечение");
        clinicService.setPrice(new BigDecimal("2500.00"));
        clinicService.setActive(true);
        AppointmentServiceItem historical = new AppointmentServiceItem();
        historical.setId(50L);
        historical.setAppointment(appointment);
        historical.setService(clinicService);
        historical.setPrice(new BigDecimal("1000.00"));
        historical.setQuantity(1);
        when(items.findByAppointmentId(10L)).thenReturn(List.of(historical));
        when(doctorService.getForAssignment(20L)).thenReturn(appointment.getDoctor());
        when(patientService.get(30L)).thenReturn(appointment.getPatient());

        var response = updateManager.update(10L,
                request(20L, List.of(new AppointmentDtos.ServiceItemRequest(5L, 2))), admin());

        assertEquals(new BigDecimal("1000.00"), response.services().getFirst().unitPrice());
        assertEquals(new BigDecimal("2000.00"), response.servicesTotal());
        verify(serviceManager, never()).getForUpdate(5L);
        verify(items, never()).deleteAll(any());
    }

    @Test
    void updateAllowsUnchangedPastStartForInProgressAppointment() {
        PatientService patientService = mock(PatientService.class);
        DoctorService doctorService = mock(DoctorService.class);
        ClinicServiceManager serviceManager = mock(ClinicServiceManager.class);
        AppointmentManager updateManager = new AppointmentManager(appointments, items, payments, patientService,
                doctorService, serviceManager, mock(UserRepository.class), doctorRepository, clinicSettings);
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setStartTime(OffsetDateTime.now().minusHours(1));
        appointment.setEndTime(OffsetDateTime.now().minusMinutes(30));
        ClinicService clinicService = clinicService(5L);
        AppointmentServiceItem historical = new AppointmentServiceItem();
        historical.setId(50L);
        historical.setAppointment(appointment);
        historical.setService(clinicService);
        historical.setPrice(new BigDecimal("1000.00"));
        historical.setQuantity(1);
        when(items.findByAppointmentId(10L)).thenReturn(List.of(historical));
        when(doctorService.getForAssignment(20L)).thenReturn(appointment.getDoctor());
        when(patientService.get(30L)).thenReturn(appointment.getPatient());
        var request = new AppointmentDtos.AppointmentRequest(30L, 20L, appointment.getStartTime(),
                appointment.getEndTime(), null, null, null,
                List.of(new AppointmentDtos.ServiceItemRequest(5L, 1)));

        assertDoesNotThrow(() -> updateManager.update(10L, request, admin()));
        verify(clinicSettings).validateAppointmentTime(appointment.getStartTime(), appointment.getEndTime());
    }

    @Test
    void createRejectsPastStartBeforeWritingAnything() {
        OffsetDateTime start = OffsetDateTime.now().minusHours(1);
        var past = new AppointmentDtos.AppointmentRequest(30L, 20L, start, start.plusMinutes(30),
                null, null, null, List.of(new AppointmentDtos.ServiceItemRequest(1L, 1)));

        assertThrows(IllegalArgumentException.class, () -> manager.create(past, admin()));
        verify(appointments, never()).save(any());
    }

    @Test
    void inactiveDoctorCannotBeAssignedToNewAppointment() {
        PatientService patientService=mock(PatientService.class); DoctorService doctorService=mock(DoctorService.class);
        ClinicServiceManager serviceManager=mock(ClinicServiceManager.class); UserRepository userRepository=mock(UserRepository.class);
        AppointmentManager createManager=new AppointmentManager(appointments,items,payments,patientService,doctorService,serviceManager,userRepository,doctorRepository,clinicSettings);
        Doctor inactive=appointment.getDoctor();inactive.getUser().setActive(false);
        when(doctorService.getForAssignment(20L)).thenReturn(inactive);when(patientService.get(30L)).thenReturn(appointment.getPatient());
        User adminUser=new User();adminUser.setUsername("admin");when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(adminUser));
        var request=new AppointmentDtos.AppointmentRequest(30L,20L,OffsetDateTime.now().plusDays(2),OffsetDateTime.now().plusDays(2).plusMinutes(30),null,null,null,List.of(new AppointmentDtos.ServiceItemRequest(1L,1)));
        assertThrows(IllegalArgumentException.class,()->createManager.create(request,admin()));
        verify(appointments,never()).save(any());
    }

    @Test
    void rejectsAppointmentWithoutServices() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.create(request(20L, List.of()), admin()));
        assertTrue(error.getMessage().contains("хотя бы одну услугу"));
        verify(appointments, never()).save(any());
    }

    @Test
    void newServicesAreLockedInStableIdOrderBeforeActiveCheck() {
        PatientService patientService = mock(PatientService.class);
        DoctorService doctorService = mock(DoctorService.class);
        ClinicServiceManager serviceManager = mock(ClinicServiceManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        AppointmentManager createManager = new AppointmentManager(appointments, items, payments, patientService,
                doctorService, serviceManager, userRepository, doctorRepository, clinicSettings);
        when(doctorService.getForAssignment(20L)).thenReturn(appointment.getDoctor());
        when(patientService.get(30L)).thenReturn(appointment.getPatient());
        User adminUser = new User(); adminUser.setUsername("admin");
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(adminUser));
        ClinicService first = clinicService(1L); ClinicService second = clinicService(2L);
        when(serviceManager.getForUpdate(1L)).thenReturn(first);
        when(serviceManager.getForUpdate(2L)).thenReturn(second);
        when(appointments.save(any())).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        createManager.create(request(20L, List.of(new AppointmentDtos.ServiceItemRequest(2L, 1),
                new AppointmentDtos.ServiceItemRequest(1L, 1))), admin());

        var order = inOrder(serviceManager);
        order.verify(serviceManager).getForUpdate(1L);
        order.verify(serviceManager).getForUpdate(2L);
    }

    private ClinicService clinicService(Long id) {
        ClinicService service = new ClinicService();
        service.setId(id); service.setName("Услуга " + id); service.setPrice(BigDecimal.TEN); service.setActive(true);
        return service;
    }

    private AppointmentServiceItem serviceItem(String price, int quantity) {
        AppointmentServiceItem item = new AppointmentServiceItem();
        item.setPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }

    private AppointmentDtos.AppointmentRequest request(Long doctorId,
                                                        List<AppointmentDtos.ServiceItemRequest> services) {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        return new AppointmentDtos.AppointmentRequest(30L, doctorId, start, start.plusMinutes(30),
                null, null, null, services);
    }

    private Appointment appointment(Long id, Long doctorId, AppointmentStatus status) {
        User user = new User();
        user.setId(doctorId);
        user.setUsername("doctor");
        user.setFullName("Doctor");
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setUser(user);
        Patient patient = new Patient();
        patient.setId(30L);
        patient.setFullName("Patient");
        patient.setPhone("123");
        Appointment result = new Appointment();
        result.setId(id);
        result.setDoctor(doctor);
        result.setPatient(patient);
        result.setStatus(status);
        result.setStartTime(OffsetDateTime.now().plusDays(1));
        result.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        result.setCreatedBy(user);
        return result;
    }

    private Authentication admin() {
        return new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Authentication doctor() {
        return new UsernamePasswordAuthenticationToken("doctor", "", List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
    }
}
