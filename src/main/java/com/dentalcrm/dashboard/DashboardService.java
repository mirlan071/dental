package com.dentalcrm.dashboard;

import com.dentalcrm.appointment.*;
import com.dentalcrm.common.NotFoundException;
import com.dentalcrm.doctor.*;
import com.dentalcrm.patient.*;
import com.dentalcrm.payment.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;

import static com.dentalcrm.dashboard.DashboardDtos.*;

@Service
public class DashboardService {
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final AppointmentServiceItemRepository serviceItems;
    private final PaymentRepository payments;
    private final DoctorRepository doctors;

    public DashboardService(PatientRepository patients, AppointmentRepository appointments,
                            AppointmentServiceItemRepository serviceItems, PaymentRepository payments,
                            DoctorRepository doctors) {
        this.patients = patients;
        this.appointments = appointments;
        this.serviceItems = serviceItems;
        this.payments = payments;
        this.doctors = doctors;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(OffsetDateTime from, OffsetDateTime to) {
        validateRange(from, to);
        var period = appointments.findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(from, to);
        var completed = period.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).toList();
        List<Payment> periodPayments = payments.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, to);
        BigDecimal cash = sum(periodPayments, PaymentMethod.CASH);
        BigDecimal card = sum(periodPayments, PaymentMethod.CARD);
        BigDecimal qr = sum(periodPayments, PaymentMethod.QR);
        BigDecimal performed = completed.stream().map(this::appointmentTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = cash.add(card).add(qr);
        BigDecimal periodOutstanding = completed.stream().map(this::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DebtorSummary> debtors = debtors(null);
        BigDecimal totalDebt = debtors.stream().map(DebtorSummary::totalDebt).reduce(BigDecimal.ZERO, BigDecimal::add);
        long periodPatients = period.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .map(a -> a.getPatient().getId()).distinct().count();

        return new DashboardSummary(from, to, periodPatients, period.size(), completed.size(),
                period.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count(),
                received, cash, card, qr, performed, received, periodOutstanding,
                debtors.size(), totalDebt, doctorPerformance(from, to));
    }

    @Transactional(readOnly = true)
    public DoctorDashboardSummary doctorSummary(String username, OffsetDateTime from, OffsetDateTime to) {
        validateRange(from, to);
        var doctor = doctors.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден."));
        List<Appointment> periodAppointments = appointments
                .findByDoctorIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(doctor.getId(), from, to);
        List<Payment> periodPayments = payments
                .findByAppointmentDoctorIdAndPaidAtGreaterThanEqualAndPaidAtLessThan(doctor.getId(), from, to);
        long patientCount = periodAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .map(a -> a.getPatient().getId()).distinct().count();
        long completedCount = periodAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        BigDecimal cash = sum(periodPayments, PaymentMethod.CASH);
        BigDecimal card = sum(periodPayments, PaymentMethod.CARD);
        BigDecimal qr = sum(periodPayments, PaymentMethod.QR);
        BigDecimal revenue = cash.add(card).add(qr);
        BigDecimal performed = periodAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(this::appointmentTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageCheck = completedCount == 0 ? BigDecimal.ZERO.setScale(2)
                : performed.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP);
        return new DoctorDashboardSummary(from, to, patientCount, completedCount, revenue, averageCheck,
                new DoctorPaymentBreakdown(cash, card, qr));
    }

    @Transactional(readOnly = true)
    public List<DebtorSummary> debtors(String search) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return currentDebtAppointments().stream().collect(java.util.stream.Collectors.groupingBy(a -> a.getPatient().getId()))
                .values().stream().map(this::debtorSummary)
                .filter(d -> term.isBlank() || d.patientFullName().toLowerCase(Locale.ROOT).contains(term)
                        || d.phone().toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing(DebtorSummary::totalDebt).reversed()
                        .thenComparing(DebtorSummary::lastTreatmentDate, Comparator.reverseOrder())).toList();
    }

    @Transactional(readOnly = true)
    public PatientDebtDetails patientDebt(Long patientId) {
        Patient patient = patients.findById(patientId).orElseThrow(() -> new NotFoundException("Пациент не найден: " + patientId));
        List<Appointment> debtAppointments = currentDebtAppointments().stream()
                .filter(a -> a.getPatient().getId().equals(patientId)).toList();
        List<DebtAppointment> details = debtAppointments.stream().map(this::debtAppointment).toList();
        return new PatientDebtDetails(patient.getId(), patient.getFullName(), patient.getPhone(),
                details.stream().map(DebtAppointment::appointmentTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                details.stream().map(DebtAppointment::paid).reduce(BigDecimal.ZERO, BigDecimal::add),
                details.stream().map(DebtAppointment::remainingBalance).reduce(BigDecimal.ZERO, BigDecimal::add), details);
    }

    private List<DoctorPerformance> doctorPerformance(OffsetDateTime from, OffsetDateTime to) {
        return doctors.findAllByOrderByUserFullName().stream().map(doctor -> {
            List<Appointment> period = appointments
                    .findByDoctorIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(doctor.getId(), from, to);
            List<Appointment> completed = period.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).toList();
            long patientCount = completed.stream().map(a -> a.getPatient().getId()).distinct().count();
            BigDecimal performed = completed.stream().map(this::appointmentTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal received = payments
                    .findByAppointmentDoctorIdAndPaidAtGreaterThanEqualAndPaidAtLessThan(doctor.getId(), from, to)
                    .stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = completed.stream().map(this::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new DoctorPerformance(doctor.getId(), doctor.getUser().getFullName(), patientCount,
                    completed.size(), performed, received, outstanding);
        }).filter(result -> result.completedAppointments() > 0 || result.paymentsReceived().signum() > 0)
                .sorted(Comparator.comparing(DoctorPerformance::servicesPerformed).reversed()).toList();
    }

    private List<Appointment> currentDebtAppointments() {
        return appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED).stream()
                .filter(a -> remaining(a).compareTo(BigDecimal.ZERO) > 0).toList();
    }

    private DebtorSummary debtorSummary(List<Appointment> source) {
        Appointment newest = source.stream().max(Comparator.comparing(Appointment::getStartTime)).orElseThrow();
        List<DebtorDoctor> involved = source.stream().map(Appointment::getDoctor)
                .collect(java.util.stream.Collectors.toMap(Doctor::getId,
                        d -> new DebtorDoctor(d.getId(), d.getUser().getFullName()), (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
        BigDecimal treatment = source.stream().map(this::appointmentTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = source.stream().map(this::paidTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DebtorSummary(newest.getPatient().getId(), newest.getPatient().getFullName(), newest.getPatient().getPhone(),
                treatment, paid, treatment.subtract(paid).max(BigDecimal.ZERO), newest.getStartTime(), source.size(), involved);
    }

    private DebtAppointment debtAppointment(Appointment appointment) {
        List<AppointmentServiceItem> items = serviceItems.findByAppointmentId(appointment.getId());
        BigDecimal total = items.stream().map(AppointmentServiceItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = paidTotal(appointment);
        return new DebtAppointment(appointment.getId(), appointment.getStartTime(), appointment.getDoctor().getId(),
                appointment.getDoctor().getUser().getFullName(), items.stream().map(i -> i.getService().getName()).toList(),
                total, paid, total.subtract(paid).max(BigDecimal.ZERO), appointment.getStatus());
    }

    private BigDecimal appointmentTotal(Appointment appointment) {
        return serviceItems.findByAppointmentId(appointment.getId()).stream().map(AppointmentServiceItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Payment> paymentList(Appointment appointment) { return payments.findByAppointmentIdOrderByPaidAt(appointment.getId()); }
    private BigDecimal paidTotal(Appointment appointment) { return paymentList(appointment).stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private BigDecimal remaining(Appointment appointment) { return appointmentTotal(appointment).subtract(paidTotal(appointment)).max(BigDecimal.ZERO); }
    private BigDecimal sum(List<Payment> source, PaymentMethod method) { return source.stream().filter(p -> p.getPaymentMethod() == method).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private void validateRange(OffsetDateTime from, OffsetDateTime to) { if (!to.isAfter(from)) throw new IllegalArgumentException("Конец периода должен быть позже начала."); }
}
