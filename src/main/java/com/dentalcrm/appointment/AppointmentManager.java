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
        List<ServiceItemRequest> requestedServices = requestedServices(request);
        requireServices(requestedServices);
        if (request.startTime().toInstant().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Нельзя создать запись в прошлом.");
        }
        validateTime(request.startTime(), request.endTime());
        if (request.status() != null && request.status() != AppointmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("Новая запись должна иметь статус SCHEDULED.");
        }
        ensureDoctorAccess(request.doctorId(), auth);
        checkConflict(request.doctorId(), request.startTime(), request.endTime(), null);

        Appointment appointment = new Appointment();
        appointment.setPatient(patients.get(request.patientId()));
        Doctor selectedDoctor = doctors.getForAssignment(request.doctorId());
        if (!selectedDoctor.getUser().isActive()) throw new IllegalArgumentException("Нельзя назначить неактивного врача.");
        appointment.setDoctor(selectedDoctor);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes(request.notes());
        appointment.setCreatedBy(users.findByUsernameIgnoreCase(auth.getName()).orElseThrow());
        repo.save(appointment);
        addServices(appointment, requestedServices);
        return map(appointment);
    }

    public AppointmentResponse update(Long id, AppointmentRequest request, Authentication auth) {
        Appointment appointment = getForUpdate(id);
        ensureDoctorAccess(appointment.getDoctor().getId(), auth);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new ConflictException("Завершённую или отменённую запись нельзя редактировать.");
        }
        List<ServiceItemRequest> requestedServices = requestedServices(request);
        requireServices(requestedServices);
        if (request.startTime().toInstant().isBefore(Instant.now())
                && !request.startTime().toInstant().equals(appointment.getStartTime().toInstant())) {
            throw new IllegalArgumentException("Нельзя перенести запись в прошлое.");
        }
        ensureDoctorAccess(request.doctorId(), auth);
        validateTime(request.startTime(), request.endTime());
        checkConflict(request.doctorId(), request.startTime(), request.endTime(), id);

        appointment.setPatient(patients.get(request.patientId()));
        Doctor selectedDoctor = doctors.getForAssignment(request.doctorId());
        if (!selectedDoctor.getUser().isActive()) {
            throw new IllegalArgumentException("Нельзя назначить неактивного врача.");
        }
        appointment.setDoctor(selectedDoctor);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setNotes(request.notes());
        replaceServices(appointment, requestedServices);
        if (paidTotal(id).compareTo(servicesTotal(id)) > 0) {
            throw new ConflictException("Стоимость услуг не может быть меньше уже оплаченной суммы.");
        }
        return map(appointment);
    }

    public AppointmentResponse status(Long id, AppointmentStatus target, Authentication auth) {
        Appointment appointment = getForUpdate(id);
        ensureDoctorAccess(appointment.getDoctor().getId(), auth);
        AppointmentStatus current = appointment.getStatus();
        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new ConflictException("Такой переход статуса приёма недоступен.");
        }
        if (target == AppointmentStatus.COMPLETED && !items.existsByAppointmentId(id)) {
            throw new ConflictException("Нельзя завершить приём без услуг.");
        }
        appointment.setStatus(target);
        return map(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(OffsetDateTime from, OffsetDateTime to, Authentication auth) {
        if (!to.isAfter(from)) throw new IllegalArgumentException("Конец периода должен быть позже начала.");
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        var data = admin
                ? repo.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(from, to)
                : repo.findByDoctorIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                        doctorProfile(auth).getId(), from, to);
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
        Map<Long, ClinicService> lockedServices = lockServices(requested);
        Set<Long> seen = new HashSet<>();
        for (ServiceItemRequest request : requested) {
            if (!seen.add(request.serviceId())) throw new ConflictException("Одну услугу нельзя добавить в запись дважды.");
            ClinicService service = lockedServices.get(request.serviceId());
            if (!service.isActive()) throw new IllegalArgumentException("Нельзя добавить неактивную услугу: " + request.serviceId());
            AppointmentServiceItem item = new AppointmentServiceItem();
            item.setAppointment(appointment);
            item.setService(service);
            item.setPrice(service.getPrice());
            item.setQuantity(request.quantity() == null ? 1 : request.quantity());
            items.save(item);
        }
        items.flush();
    }

    private void replaceServices(Appointment appointment, List<ServiceItemRequest> requested) {
        List<AppointmentServiceItem> current = items.findByAppointmentId(appointment.getId());
        Map<Long, AppointmentServiceItem> existing = new HashMap<>();
        current.forEach(item -> existing.put(item.getService().getId(), item));
        Set<Long> requestedIds = new HashSet<>();
        List<ServiceItemRequest> added = requested.stream()
                .filter(request -> !existing.containsKey(request.serviceId()))
                .toList();
        Map<Long, ClinicService> lockedServices = lockServices(added);
        for (ServiceItemRequest request : requested) {
            if (!requestedIds.add(request.serviceId())) {
                throw new ConflictException("Одну услугу нельзя добавить в запись дважды.");
            }
            AppointmentServiceItem item = existing.get(request.serviceId());
            if (item != null) {
                item.setQuantity(request.quantity() == null ? 1 : request.quantity());
            } else {
                ClinicService service = lockedServices.get(request.serviceId());
                if (!service.isActive()) throw new IllegalArgumentException("Нельзя добавить неактивную услугу: " + request.serviceId());
                AppointmentServiceItem newItem = new AppointmentServiceItem();
                newItem.setAppointment(appointment);
                newItem.setService(service);
                newItem.setPrice(service.getPrice());
                newItem.setQuantity(request.quantity() == null ? 1 : request.quantity());
                items.save(newItem);
            }
        }
        List<AppointmentServiceItem> removed = current.stream()
                .filter(item -> !requestedIds.contains(item.getService().getId()))
                .toList();
        if (!removed.isEmpty()) items.deleteAll(removed);
        items.flush();
    }

    private Map<Long, ClinicService> lockServices(List<ServiceItemRequest> requested) {
        Map<Long, ClinicService> locked = new HashMap<>();
        requested.stream().map(ServiceItemRequest::serviceId).distinct().sorted()
                .forEach(id -> locked.put(id, services.getForUpdate(id)));
        return locked;
    }

    private List<ServiceItemRequest> requestedServices(AppointmentRequest request) {
        if (request.services() != null) return request.services();
        if (request.serviceIds() == null) return List.of();
        return request.serviceIds().stream().map(id -> new ServiceItemRequest(id, 1)).toList();
    }

    private void requireServices(List<ServiceItemRequest> requested) {
        if (requested.isEmpty()) throw new IllegalArgumentException("Добавьте хотя бы одну услугу.");
    }

    private void checkConflict(Long doctorId, OffsetDateTime start, OffsetDateTime end, Long excludedId) {
        if (repo.hasConflict(doctorId, start, end, excludedId)) {
            throw new ConflictException("У врача уже есть запись на это время.");
        }
    }

    private void validateTime(OffsetDateTime start, OffsetDateTime end) {
        if (!end.isAfter(start)) throw new IllegalArgumentException("Время окончания должно быть позже времени начала.");
        clinicSettings.validateAppointmentTime(start, end);
    }

    private void ensureDoctorAccess(Long doctorId, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !doctorProfile(auth).getId().equals(doctorId)) {
            throw new AccessDeniedException("Врач может работать только со своими приёмами.");
        }
    }

    private Doctor doctorProfile(Authentication auth) {
        return doctorRepo.findByUserUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Профиль врача не настроен."));
    }

    private Appointment get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Приём не найден: " + id));
    }

    private Appointment getForUpdate(Long id) {
        return repo.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Приём не найден: " + id));
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
