package com.dentalcrm.appointment;

import com.dentalcrm.common.*;
import com.dentalcrm.doctor.*;
import com.dentalcrm.patient.*;
import com.dentalcrm.payment.*;
import com.dentalcrm.service.*;
import com.dentalcrm.settings.ClinicSettingsService;
import com.dentalcrm.user.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static com.dentalcrm.appointment.AppointmentDtos.*;

@Service
@Transactional
public class AppointmentManager {
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            AppointmentStatus.SCHEDULED, Set.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
            AppointmentStatus.IN_PROGRESS, Set.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED),
            AppointmentStatus.COMPLETED, Set.of(),
            AppointmentStatus.CANCELLED, Set.of(),
            AppointmentStatus.NO_SHOW, Set.of()
    );

    private final AppointmentRepository repo;
    private final AppointmentServiceItemRepository items;
    private final PaymentRepository payments;
    private final PatientService patients;
    private final DoctorService doctors;
    private final ClinicServiceManager services;
    private final UserRepository users;
    private final DoctorRepository doctorRepo;
    private final ClinicSettingsService clinicSettings;

    public AppointmentManager(AppointmentRepository repo, AppointmentServiceItemRepository items,
                              PaymentRepository payments, PatientService patients, DoctorService doctors,
                              ClinicServiceManager services, UserRepository users, DoctorRepository doctorRepo,
                              ClinicSettingsService clinicSettings) {
        this.repo = repo;
        this.items = items;
        this.payments = payments;
        this.patients = patients;
        this.doctors = doctors;
        this.services = services;
        this.users = users;
        this.doctorRepo = doctorRepo;
        this.clinicSettings = clinicSettings;
    }

    public AppointmentResponse create(AppointmentRequest request, Authentication auth) {
        validateTime(request.startTime(), request.endTime());
        if (request.status() != null && request.status() != AppointmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("New appointments must have SCHEDULED status");
        }
        ensureDoctorAccess(request.doctorId(), auth);
        checkConflict(request.doctorId(), request.startTime(), request.endTime(), null);

        Appointment appointment = new Appointment();
        appointment.setPatient(patients.get(request.patientId()));
        Doctor selectedDoctor = doctors.get(request.doctorId());
        if (!selectedDoctor.getUser().isActive()) throw new IllegalArgumentException("Inactive doctor cannot be assigned to a new appointment");
        appointment.setDoctor(selectedDoctor);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes(request.notes());
        appointment.setCreatedBy(users.findByUsernameIgnoreCase(auth.getName()).orElseThrow());
        repo.save(appointment);
        addServices(appointment, requestedServices(request));
        return map(appointment);
    }

    public AppointmentResponse update(Long id, AppointmentRequest request, Authentication auth) {
        Appointment appointment = get(id);
        ensureDoctorAccess(appointment.getDoctor().getId(), auth);
        ensureDoctorAccess(request.doctorId(), auth);
        validateTime(request.startTime(), request.endTime());
        checkConflict(request.doctorId(), request.startTime(), request.endTime(), id);

        appointment.setPatient(patients.get(request.patientId()));
        appointment.setDoctor(doctors.get(request.doctorId()));
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setNotes(request.notes());
        items.deleteAll(items.findByAppointmentId(id));
        items.flush();
        addServices(appointment, requestedServices(request));
        if (paidTotal(id).compareTo(servicesTotal(id)) > 0) {
            throw new ConflictException("Appointment services total cannot be lower than the amount already paid");
        }
        return map(appointment);
    }

    public AppointmentResponse status(Long id, AppointmentStatus target, Authentication auth) {
        Appointment appointment = get(id);
        ensureDoctorAccess(appointment.getDoctor().getId(), auth);
        AppointmentStatus current = appointment.getStatus();
        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new ConflictException("Status transition from " + current + " to " + target + " is not allowed");
        }
        if (target == AppointmentStatus.COMPLETED && !items.existsByAppointmentId(id)) {
            throw new ConflictException("Appointment cannot be completed without at least one service");
        }
        appointment.setStatus(target);
        return map(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(OffsetDateTime from, OffsetDateTime to, Authentication auth) {
        if (!to.isAfter(from)) throw new IllegalArgumentException("to must be after from");
        var doctor = doctorRepo.findByUserUsername(auth.getName());
        var data = doctor.isPresent()
                ? repo.findByDoctorIdAndStartTimeBetweenOrderByStartTime(doctor.get().getId(), from, to)
                : repo.findByStartTimeBetweenOrderByStartTime(from, to);
        return data.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse find(Long id, Authentication auth) {
        Appointment appointment = get(id);
        ensureDoctorAccess(appointment.getDoctor().getId(), auth);
        return map(appointment);
    }

    public BigDecimal servicesTotal(Long appointmentId) {
        return items.findByAppointmentId(appointmentId).stream()
                .map(AppointmentServiceItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal paidTotal(Long appointmentId) {
        return payments.findByAppointmentIdOrderByPaidAt(appointmentId).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addServices(Appointment appointment, List<ServiceItemRequest> requested) {
        Set<Long> seen = new HashSet<>();
        for (ServiceItemRequest request : requested) {
            if (!seen.add(request.serviceId())) throw new ConflictException("A service may appear only once per appointment");
            ClinicService service = services.get(request.serviceId());
            if (!service.isActive()) throw new IllegalArgumentException("Inactive service cannot be added: " + request.serviceId());
            AppointmentServiceItem item = new AppointmentServiceItem();
            item.setAppointment(appointment);
            item.setService(service);
            item.setPrice(service.getPrice());
            item.setQuantity(request.quantity() == null ? 1 : request.quantity());
            items.save(item);
        }
        items.flush();
    }

    private List<ServiceItemRequest> requestedServices(AppointmentRequest request) {
        if (request.services() != null) return request.services();
        if (request.serviceIds() == null) return List.of();
        return request.serviceIds().stream().map(id -> new ServiceItemRequest(id, 1)).toList();
    }

    private void checkConflict(Long doctorId, OffsetDateTime start, OffsetDateTime end, Long excludedId) {
        if (repo.hasConflict(doctorId, start, end, excludedId)) {
            throw new ConflictException("Doctor already has an overlapping appointment");
        }
    }

    private void validateTime(OffsetDateTime start, OffsetDateTime end) {
        if (!end.isAfter(start)) throw new IllegalArgumentException("endTime must be after startTime");
        clinicSettings.validateAppointmentTime(start, end);
    }

    private void ensureDoctorAccess(Long doctorId, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && doctorRepo.findByUserUsername(auth.getName()).map(Doctor::getId).filter(doctorId::equals).isEmpty()) {
            throw new AccessDeniedException("Doctors may only manage their own appointments");
        }
    }

    private Appointment get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
    }

    private AppointmentResponse map(Appointment appointment) {
        List<ServiceItemResponse> serviceResponses = items.findByAppointmentId(appointment.getId()).stream()
                .map(item -> new ServiceItemResponse(item.getId(), item.getService().getId(), item.getService().getName(),
                        item.getQuantity(), item.getPrice(), item.lineTotal()))
                .toList();
        BigDecimal servicesTotal = serviceResponses.stream().map(ServiceItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PaymentItemResponse> paymentResponses = payments.findByAppointmentIdOrderByPaidAt(appointment.getId()).stream()
                .map(payment -> new PaymentItemResponse(payment.getId(), payment.getAmount(),
                        payment.getPaymentMethod().name(), payment.getPaidAt()))
                .toList();
        BigDecimal paidTotal = paymentResponses.stream().map(PaymentItemResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AppointmentResponse(appointment.getId(), appointment.getPatient().getId(), appointment.getPatient().getFullName(),
                appointment.getDoctor().getId(), appointment.getDoctor().getUser().getFullName(), appointment.getStartTime(),
                appointment.getEndTime(), appointment.getStatus(), appointment.getNotes(), appointment.getCreatedAt(),
                appointment.getCreatedBy().getUsername(), serviceResponses, servicesTotal, paymentResponses, paidTotal,
                servicesTotal.subtract(paidTotal));
    }
}
