package com.dentalcrm.payment;
import org.springframework.data.jpa.repository.*; import java.time.OffsetDateTime; import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment,Long>{ List<Payment> findByAppointmentIdOrderByPaidAt(Long appointmentId); List<Payment> findByPaidAtGreaterThanEqualAndPaidAtLessThan(OffsetDateTime from,OffsetDateTime to); List<Payment> findByAppointmentDoctorIdAndPaidAtGreaterThanEqualAndPaidAtLessThan(Long doctorId,OffsetDateTime from,OffsetDateTime to); }
