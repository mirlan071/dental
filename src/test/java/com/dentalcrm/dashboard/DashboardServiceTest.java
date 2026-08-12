package com.dentalcrm.dashboard;

import com.dentalcrm.appointment.*;
import com.dentalcrm.doctor.*;
import com.dentalcrm.patient.*;
import com.dentalcrm.payment.*;
import com.dentalcrm.service.ClinicService;
import com.dentalcrm.user.User;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private PatientRepository patients;
    private AppointmentRepository appointments;
    private AppointmentServiceItemRepository items;
    private PaymentRepository payments;
    private DoctorRepository doctors;
    private DashboardService service;
    private OffsetDateTime from;
    private OffsetDateTime to;

    @BeforeEach void setUp() {
        patients=mock(PatientRepository.class); appointments=mock(AppointmentRepository.class);
        items=mock(AppointmentServiceItemRepository.class); payments=mock(PaymentRepository.class);
        doctors=mock(DoctorRepository.class); service=new DashboardService(patients,appointments,items,payments,doctors);
        from=OffsetDateTime.parse("2026-08-01T00:00:00+06:00"); to=from.plusMonths(1);
        when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of());
        when(doctors.findByUserActiveTrueOrderByUserFullName()).thenReturn(List.of());
    }

    @Test void fullyPaidAppointmentIsNotDebtAndFinalPaymentRemovesPatient() {
        Appointment a=appointment(1,patient(1,"Айжан","0555"),doctor(1,"Аида"),AppointmentStatus.COMPLETED);
        treatment(a,"10000"); paid(a,"10000",PaymentMethod.CASH);
        when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of(a));
        assertTrue(service.debtors(null).isEmpty());
    }

    @Test void partialPaymentAndLaterPaymentReduceDebt() {
        Appointment a=appointment(1,patient(1,"Айжан","0555"),doctor(1,"Аида"),AppointmentStatus.COMPLETED);
        treatment(a,"10000"); paid(a,"4000",PaymentMethod.CASH);
        when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of(a));
        assertEquals(new BigDecimal("6000"),service.debtors(null).getFirst().totalDebt());
        paid(a,"4000",PaymentMethod.CASH,"3000",PaymentMethod.CARD);
        assertEquals(new BigDecimal("3000"),service.debtors(null).getFirst().totalDebt());
    }

    @Test void zeroPaymentCreatesFullDebtAndEmptyListIsSafe() {
        Appointment a=appointment(1,patient(1,"Айжан","0555"),doctor(1,"Аида"),AppointmentStatus.COMPLETED);
        treatment(a,"10000"); when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of(a));
        assertEquals(new BigDecimal("10000"),service.debtors(null).getFirst().totalDebt());
        when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of());
        assertTrue(service.debtors(null).isEmpty());
    }

    @Test void patientWithMultipleDoctorsIsAggregatedWithoutWrongAttribution() {
        Patient p=patient(1,"Бекзат","0700"); Doctor aida=doctor(1,"Аида"); Doctor azamat=doctor(2,"Азамат");
        Appointment first=appointment(1,p,aida,AppointmentStatus.COMPLETED); Appointment second=appointment(2,p,azamat,AppointmentStatus.COMPLETED);
        treatment(first,"10000");paid(first,"5000",PaymentMethod.CASH);treatment(second,"8000");paid(second,"3000",PaymentMethod.QR);
        when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of(second,first));
        var result=service.debtors(null).getFirst();
        assertEquals(new BigDecimal("18000"),result.totalTreatmentAmount()); assertEquals(new BigDecimal("10000"),result.totalDebt());
        assertEquals(2,result.unpaidAppointments()); assertEquals(Set.of("Аида","Азамат"),result.doctors().stream().map(DashboardDtos.DebtorDoctor::fullName).collect(java.util.stream.Collectors.toSet()));
    }

    @Test void onlyCompletedAppointmentsCanCreateDebt() {
        for(AppointmentStatus status:List.of(AppointmentStatus.SCHEDULED,AppointmentStatus.IN_PROGRESS,AppointmentStatus.CANCELLED,AppointmentStatus.NO_SHOW)){
            Appointment a=appointment(1,patient(1,"П","1"),doctor(1,"Д"),status);treatment(a,"1000");
            when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of());
            assertTrue(service.debtors(null).isEmpty());
        }
    }

    @Test void summaryHasCurrentDebtPeriodPerformanceDoctorAttributionAndPaymentMethods() {
        Patient p=patient(1,"Бекзат","0700");Doctor d1=doctor(1,"Аида"),d2=doctor(2,"Азамат");
        Appointment a=appointment(1,p,d1,AppointmentStatus.COMPLETED);treatment(a,"10000");paid(a,"4000",PaymentMethod.CASH,"1000",PaymentMethod.CARD,"500",PaymentMethod.QR);
        when(appointments.findByStartTimeBetweenOrderByStartTime(from,to)).thenReturn(List.of(a));
        when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of(a));
        when(doctors.findByUserActiveTrueOrderByUserFullName()).thenReturn(List.of(d1,d2));
        when(appointments.findByDoctorIdAndStartTimeBetweenOrderByStartTime(1L,from,to)).thenReturn(List.of(a));
        when(appointments.findByDoctorIdAndStartTimeBetweenOrderByStartTime(2L,from,to)).thenReturn(List.of());
        var result=service.summary(from,to);
        assertEquals(new BigDecimal("10000"),result.servicesPerformed()); assertEquals(new BigDecimal("5500"),result.paymentsReceived());
        assertEquals(new BigDecimal("4500"),result.totalDebt()); assertEquals(1,result.debtorCount());
        assertEquals(new BigDecimal("4000"),result.cashRevenue());assertEquals(new BigDecimal("1000"),result.cardRevenue());assertEquals(new BigDecimal("500"),result.qrRevenue());
        assertEquals(new BigDecimal("4500"),result.doctors().getFirst().outstandingAmount());assertEquals(BigDecimal.ZERO,result.doctors().getLast().outstandingAmount());
    }

    @Test void debtorSearchMatchesNameAndPhone() {
        Appointment a=appointment(1,patient(1,"Бекзат уулу","055934"),doctor(1,"Аида"),AppointmentStatus.COMPLETED);treatment(a,"100");
        when(appointments.findByStatusOrderByStartTimeDesc(AppointmentStatus.COMPLETED)).thenReturn(List.of(a));
        assertEquals(1,service.debtors("бекзат").size());assertEquals(1,service.debtors("5934").size());assertTrue(service.debtors("другой").isEmpty());
    }

    private Patient patient(long id,String name,String phone){Patient p=new Patient();p.setId(id);p.setFullName(name);p.setPhone(phone);return p;}
    private Doctor doctor(long id,String name){User u=new User();u.setFullName(name);Doctor d=new Doctor();d.setId(id);d.setUser(u);return d;}
    private Appointment appointment(long id,Patient p,Doctor d,AppointmentStatus status){Appointment a=new Appointment();a.setId(id);a.setPatient(p);a.setDoctor(d);a.setStatus(status);a.setStartTime(from.plusDays(id));return a;}
    private void treatment(Appointment a,String amount){AppointmentServiceItem i=new AppointmentServiceItem();i.setPrice(new BigDecimal(amount));i.setQuantity(1);ClinicService s=new ClinicService();s.setName("Лечение");i.setService(s);when(items.findByAppointmentId(a.getId())).thenReturn(List.of(i));}
    private void paid(Appointment a,Object... values){List<Payment> result=new ArrayList<>();for(int i=0;i<values.length;i+=2){Payment p=new Payment();p.setAmount(new BigDecimal((String)values[i]));p.setPaymentMethod((PaymentMethod)values[i+1]);result.add(p);}when(payments.findByAppointmentIdOrderByPaidAt(a.getId())).thenReturn(result);}
}
