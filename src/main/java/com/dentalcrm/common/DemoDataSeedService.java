package com.dentalcrm.common;

import com.dentalcrm.appointment.Appointment;
import com.dentalcrm.appointment.AppointmentRepository;
import com.dentalcrm.appointment.AppointmentServiceItem;
import com.dentalcrm.appointment.AppointmentServiceItemRepository;
import com.dentalcrm.appointment.AppointmentStatus;
import com.dentalcrm.doctor.Doctor;
import com.dentalcrm.doctor.DoctorRepository;
import com.dentalcrm.patient.Patient;
import com.dentalcrm.patient.PatientRepository;
import com.dentalcrm.payment.Payment;
import com.dentalcrm.payment.PaymentMethod;
import com.dentalcrm.payment.PaymentRepository;
import com.dentalcrm.service.ClinicService;
import com.dentalcrm.service.ClinicServiceRepository;
import com.dentalcrm.user.Role;
import com.dentalcrm.user.User;
import com.dentalcrm.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DemoDataSeedService {
    static final String SEED_KEY = "realistic-demo-dataset-v2";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Bishkek");
    private static final String DEMO_DOCTOR_PASSWORD = "DemoClinic-2026!";

    private final UserRepository users;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final ClinicServiceRepository services;
    private final AppointmentRepository appointments;
    private final AppointmentServiceItemRepository appointmentServices;
    private final PaymentRepository payments;
    private final DemoDataSeedMarkerRepository seedMarkers;
    private final PasswordEncoder encoder;

    public DemoDataSeedService(UserRepository users, DoctorRepository doctors, PatientRepository patients,
                               ClinicServiceRepository services, AppointmentRepository appointments,
                               AppointmentServiceItemRepository appointmentServices, PaymentRepository payments,
                               DemoDataSeedMarkerRepository seedMarkers, PasswordEncoder encoder) {
        this.users = users;
        this.doctors = doctors;
        this.patients = patients;
        this.services = services;
        this.appointments = appointments;
        this.appointmentServices = appointmentServices;
        this.payments = payments;
        this.seedMarkers = seedMarkers;
        this.encoder = encoder;
    }

    @Transactional
    public SeedSummary seed(User appointmentCreator) {
        if (seedMarkers.existsById(SEED_KEY)) {
            return SeedSummary.alreadyPresent();
        }
        if (appointmentCreator.getRole() != Role.ADMIN || appointmentCreator.getId() == null) {
            throw new IllegalArgumentException("Demo appointments require a persisted ADMIN as their creator");
        }

        DoctorSeedResult doctorResult = ensureDoctors();
        EntitySeedResult<ClinicService> serviceResult = ensureServices();
        EntitySeedResult<Patient> patientResult = ensurePatients();
        AppointmentSeedStats appointmentStats = createAppointments(
                doctorResult.activeDoctors(), serviceResult.entities(), patientResult.entities(), appointmentCreator);

        // Written last in the transaction: a failed seed is fully rolled back and can be retried safely.
        seedMarkers.save(new DemoDataSeedMarker(SEED_KEY));
        return new SeedSummary(false, doctorResult.created(), serviceResult.created(), patientResult.created(),
                appointmentStats.appointments, appointmentStats.payments);
    }

    private DoctorSeedResult ensureDoctors() {
        List<DoctorSpec> specs = List.of(
                new DoctorSpec("demo.aizada", "Айзада Омурова", "Стоматолог-терапевт", "+996 555 110 101", true),
                new DoctorSpec("demo.azamat", "Азамат Токтосунов", "Стоматолог-хирург", "+996 555 110 102", true),
                new DoctorSpec("demo.ruslan", "Руслан Абдыкадыров", "Ортодонт", "+996 555 110 103", true),
                new DoctorSpec("demo.aibek", "Айбек Маматов", "Стоматолог-ортопед", "+996 555 110 104", true),
                new DoctorSpec("demo.nuriza", "Нуриза Сыдыкова", "Детский стоматолог", "+996 555 110 105", true),
                new DoctorSpec("demo.bakyt", "Бакыт Ибраев", "Стоматолог-терапевт", "+996 555 110 199", false));

        List<Doctor> activeDoctors = new ArrayList<>();
        int created = 0;
        for (DoctorSpec spec : specs) {
            Optional<User> existingAccount = users.findByUsernameIgnoreCase(spec.username());
            Doctor doctor;
            if (existingAccount.isPresent()) {
                User account = existingAccount.get();
                doctor = doctors.findByUserUsername(account.getUsername())
                        .orElseThrow(() -> new IllegalStateException(
                                "Demo doctor username is already used by an account without a doctor profile: "
                                        + spec.username()));
                if (account.getRole() != Role.DOCTOR || account.isActive() != spec.active()) {
                    throw new IllegalStateException("Demo doctor username is already used by an incompatible account: "
                            + spec.username());
                }
            } else {
                User account = users.save(user(spec));
                doctor = doctors.save(doctor(account, spec.specialization(), spec.phone()));
                created++;
            }
            if (spec.active()) {
                activeDoctors.add(doctor);
            }
        }
        return new DoctorSeedResult(activeDoctors, created);
    }

    private EntitySeedResult<ClinicService> ensureServices() {
        List<ServiceSpec> specs = List.of(
                new ServiceSpec("Консультация", new BigDecimal("500"), 30, true),
                new ServiceSpec("Профессиональная чистка", new BigDecimal("2500"), 60, true),
                new ServiceSpec("Лечение кариеса", new BigDecimal("3500"), 60, true),
                new ServiceSpec("Лечение пульпита", new BigDecimal("6500"), 90, true),
                new ServiceSpec("Удаление зуба", new BigDecimal("3000"), 45, true),
                new ServiceSpec("Сложное удаление зуба", new BigDecimal("7000"), 90, true),
                new ServiceSpec("Пломбирование", new BigDecimal("2800"), 45, true),
                new ServiceSpec("Отбеливание", new BigDecimal("12000"), 120, true),
                new ServiceSpec("Установка коронки", new BigDecimal("18000"), 120, true),
                new ServiceSpec("Ортодонтическая консультация", new BigDecimal("1000"), 30, true),
                new ServiceSpec("Снятие старой пломбы", new BigDecimal("800"), 30, false));

        List<ClinicService> result = new ArrayList<>();
        int created = 0;
        for (ServiceSpec spec : specs) {
            Optional<ClinicService> existing = services.findByName(spec.name());
            if (existing.isPresent()) {
                result.add(existing.get());
            } else {
                ClinicService service = new ClinicService();
                service.setName(spec.name());
                service.setPrice(spec.price());
                service.setDurationMinutes(spec.duration());
                service.setActive(spec.active());
                result.add(services.save(service));
                created++;
            }
        }
        return new EntitySeedResult<>(result, created);
    }

    private EntitySeedResult<Patient> ensurePatients() {
        String[] names = {
                "Бекзат уулу Эрбол", "Айжан Садыкова", "Нурсултан Абдрахманов", "Алина Токтобаева",
                "Эльдар Мамытов", "Мээрим Жолдошева", "Адилет Бекназаров", "Каныкей Осмонова",
                "Темирлан Асанов", "Диана Муратова", "Бактияр Жумабаев", "Назгуль Эсенова",
                "Арсен Касымов", "Айпери Турсунова", "Данияр Султанов", "Сезим Абдыева",
                "Эрмек Кудайбергенов", "Жанара Иманалиева", "Азамат уулу Нурбек", "Элина Кубатова",
                "Улан Токтогулов", "Арууке Болотбекова", "Ислам Ташматов", "Асель Нурматова",
                "Марат Абыкеев", "Бегайым Сариева", "Эмир Чыныбаев", "Перизат Калыбекова",
                "Алмаз Шаршенов", "Нурзат Алиева", "Самат Омуралиев", "Аделя Исакова",
                "Кубаныч Боронбаев", "Жибек Асаналиева", "Медер Сыдыков", "Айдана Жээнбекова",
                "Эрлан Мамасалиев", "Малика Токтоналиева", "Султан Абдылдаев", "Чолпон Эшимова",
                "Нурбек Аматов", "Камила Дуйшенова", "Аскар уулу Баяман", "Эльвира Райымбекова",
                "Таалай Осмонов", "Салтанат Акматова", "Ильгиз Бакытов", "Анаркуль Молдобаева",
                "Ринат Кожоев", "Зарина Усенова"};
        String[] notes = {null, "Первичный приём", "Повторный пациент",
                "Предпочитает вечернее время", null, "Удобнее связаться по телефону"};

        List<Patient> result = new ArrayList<>();
        int created = 0;
        for (int i = 0; i < names.length; i++) {
            String phone = String.format("+996 55%d %03d %03d", i % 10, 200 + i, 100 + i);
            Optional<Patient> existing = patients.findByPhone(phone);
            if (existing.isPresent()) {
                result.add(existing.get());
                continue;
            }
            Patient patient = new Patient();
            patient.setFullName(names[i]);
            patient.setPhone(phone);
            if (i % 3 != 0) {
                patient.setBirthDate(LocalDate.of(1970 + (i * 7) % 35, 1 + (i * 5) % 12, 1 + (i * 11) % 27));
            }
            patient.setNotes(notes[i % notes.length]);
            result.add(patients.save(patient));
            created++;
        }
        return new EntitySeedResult<>(result, created);
    }

    private AppointmentSeedStats createAppointments(List<Doctor> activeDoctors,
                                                     List<ClinicService> clinicServices,
                                                     List<Patient> demoPatients,
                                                     User appointmentCreator) {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        ZonedDateTime now = ZonedDateTime.now(CLINIC_ZONE);
        List<Plan> plans = new ArrayList<>();
        for (int i = 0; i < 70; i++) {
            plans.add(new Plan(today.minusDays(1 + i % 30), 9 + (i / activeDoctors.size()) % 8,
                    (i % 2) * 30, Category.PAST));
        }
        for (int i = 0; i < 16; i++) {
            plans.add(new Plan(today, 9 + (i / activeDoctors.size()) * 2,
                    (i % 2) * 30, Category.TODAY));
        }
        for (int i = 0; i < 24; i++) {
            plans.add(new Plan(today.plusDays(1 + i % 14), 9 + (i / activeDoctors.size()) % 8,
                    (i % 2) * 30, Category.FUTURE));
        }

        AppointmentSeedStats stats = new AppointmentSeedStats();
        Map<Long, Integer> debtorOccurrences = new HashMap<>();
        for (int planIndex = 0; planIndex < plans.size(); planIndex++) {
            Plan plan = plans.get(planIndex);
            Doctor doctor = activeDoctors.get(planIndex % activeDoctors.size());
            Patient patient = demoPatients.get(planIndex < 12
                    ? planIndex : (planIndex == 12 ? 0 : planIndex % demoPatients.size()));
            List<ServiceSelection> selected = selectServices(planIndex, clinicServices, plan.category());
            int duration = Math.min(120, selected.stream()
                    .mapToInt(selection -> selection.service().getDurationMinutes() * selection.quantity()).sum());
            OffsetDateTime start = plan.date().atTime(plan.hour(), plan.minute())
                    .atZone(CLINIC_ZONE).toOffsetDateTime();
            OffsetDateTime end = start.plusMinutes(duration);
            AppointmentStatus status = status(plan.category(), planIndex, start, end, now.toOffsetDateTime());
            if (status != AppointmentStatus.CANCELLED && status != AppointmentStatus.NO_SHOW
                    && appointments.hasConflict(doctor.getId(), start, end, null)) {
                continue;
            }

            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctor(doctor);
            appointment.setStartTime(start);
            appointment.setEndTime(end);
            appointment.setStatus(status);
            appointment.setNotes("Демо-расписание");
            appointment.setCreatedBy(appointmentCreator);
            appointments.save(appointment);

            for (ServiceSelection selection : selected) {
                AppointmentServiceItem item = new AppointmentServiceItem();
                item.setAppointment(appointment);
                item.setService(selection.service());
                item.setPrice(selection.service().getPrice());
                item.setQuantity(selection.quantity());
                appointmentServices.save(item);
            }
            BigDecimal total = selected.stream()
                    .map(selection -> selection.service().getPrice()
                            .multiply(BigDecimal.valueOf(selection.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (status == AppointmentStatus.COMPLETED) {
                seedPayments(appointment, total, patient, debtorOccurrences, stats, planIndex);
            }
            stats.appointments++;
        }
        return stats;
    }

    private List<ServiceSelection> selectServices(int index, List<ClinicService> source, Category category) {
        List<ClinicService> active = source.stream().filter(ClinicService::isActive).toList();
        ClinicService first = category == Category.PAST && index == 0
                ? source.stream().filter(service -> !service.isActive()).findFirst().orElse(active.getFirst())
                : active.get(index % active.size());
        List<ServiceSelection> selected = new ArrayList<>();
        selected.add(new ServiceSelection(first, index % 17 == 0 ? 2 : 1));
        if (index % 5 == 0) {
            selected.add(new ServiceSelection(active.get((index + 3) % active.size()), 1));
        }
        if (index % 13 == 0) {
            selected.add(new ServiceSelection(active.get((index + 6) % active.size()), 1));
        }
        return selected.stream().collect(Collectors.toMap(selection -> selection.service().getId(),
                        selection -> selection, (firstSelection, ignored) -> firstSelection, LinkedHashMap::new))
                .values().stream().toList();
    }

    private AppointmentStatus status(Category category, int index, OffsetDateTime start,
                                     OffsetDateTime end, OffsetDateTime now) {
        if (category == Category.FUTURE) {
            return AppointmentStatus.SCHEDULED;
        }
        if (category == Category.PAST) {
            int value = index % 20;
            if (value < 14) {
                return AppointmentStatus.COMPLETED;
            }
            if (value < 17) {
                return AppointmentStatus.CANCELLED;
            }
            if (value < 19) {
                return AppointmentStatus.NO_SHOW;
            }
            return AppointmentStatus.COMPLETED;
        }
        int todayIndex = index - 70;
        if (todayIndex == 4) {
            return AppointmentStatus.CANCELLED;
        }
        if (todayIndex == 9) {
            return AppointmentStatus.NO_SHOW;
        }
        if (!start.isAfter(now) && end.isAfter(now)) {
            return AppointmentStatus.IN_PROGRESS;
        }
        return end.isBefore(now) ? AppointmentStatus.COMPLETED : AppointmentStatus.SCHEDULED;
    }

    private void seedPayments(Appointment appointment, BigDecimal total, Patient patient,
                              Map<Long, Integer> debtCounts, AppointmentSeedStats stats, int index) {
        int patientIndex = Integer.parseInt(patient.getPhone().substring(patient.getPhone().length() - 3)) - 100;
        boolean debtor = patientIndex >= 0 && patientIndex < 12
                && (patientIndex != 0 || debtCounts.getOrDefault(patient.getId(), 0) < 2);
        if (!debtor) {
            addPayment(appointment, total, method(index), appointment.getStartTime().plusMinutes(10));
            stats.payments++;
            return;
        }

        int occurrence = debtCounts.merge(patient.getId(), 1, Integer::sum);
        if (patientIndex == 2 || patientIndex == 7) {
            return;
        }
        if (patientIndex == 1) {
            addPayment(appointment, part(total, "0.30"), PaymentMethod.CASH,
                    appointment.getStartTime().plusMinutes(20));
            addPayment(appointment, part(total, "0.20"), PaymentMethod.CARD,
                    appointment.getStartTime().plusDays(2));
            addPayment(appointment, part(total, "0.10"), PaymentMethod.QR,
                    appointment.getStartTime().plusDays(5));
            stats.payments += 3;
            return;
        }
        addPayment(appointment, part(total, patientIndex % 3 == 0 ? "0.50" : "0.65"),
                method(index + occurrence), appointment.getStartTime()
                        .plusDays(patientIndex % 4 == 0 ? 3 : 0).plusMinutes(15));
        stats.payments++;
    }

    private BigDecimal part(BigDecimal total, String ratio) {
        return total.multiply(new BigDecimal(ratio)).setScale(2, RoundingMode.DOWN);
    }

    private void addPayment(Appointment appointment, BigDecimal amount, PaymentMethod method,
                            OffsetDateTime paidAt) {
        if (amount.signum() <= 0) {
            return;
        }
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        OffsetDateTime now = OffsetDateTime.now(CLINIC_ZONE);
        payment.setPaidAt(paidAt.isAfter(now) ? now : paidAt);
        payments.save(payment);
    }

    private PaymentMethod method(int index) {
        return PaymentMethod.values()[Math.floorMod(index, PaymentMethod.values().length)];
    }

    private User user(DoctorSpec spec) {
        User user = new User();
        user.setUsername(spec.username());
        user.setPasswordHash(encoder.encode(DEMO_DOCTOR_PASSWORD));
        user.setFullName(spec.fullName());
        user.setRole(Role.DOCTOR);
        user.setActive(spec.active());
        return user;
    }

    private Doctor doctor(User user, String specialization, String phone) {
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialization(specialization);
        doctor.setPhone(phone);
        return doctor;
    }

    public record SeedSummary(boolean skipped, int doctorsCreated, int servicesCreated, int patientsCreated,
                              int appointmentsCreated, int paymentsCreated) {
        static SeedSummary alreadyPresent() {
            return new SeedSummary(true, 0, 0, 0, 0, 0);
        }
    }

    private record DoctorSpec(String username, String fullName, String specialization, String phone,
                              boolean active) {
    }

    private record ServiceSpec(String name, BigDecimal price, int duration, boolean active) {
    }

    private record ServiceSelection(ClinicService service, int quantity) {
    }

    private record Plan(LocalDate date, int hour, int minute, Category category) {
    }

    private record DoctorSeedResult(List<Doctor> activeDoctors, int created) {
    }

    private record EntitySeedResult<T>(List<T> entities, int created) {
    }

    private enum Category {
        PAST, TODAY, FUTURE
    }

    private static class AppointmentSeedStats {
        private int appointments;
        private int payments;
    }
}
