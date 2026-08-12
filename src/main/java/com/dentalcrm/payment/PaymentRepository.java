package com.dentalcrm.payment;
import org.springframework.data.jpa.repository.*; import java.time.OffsetDateTime; import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment,Long>{ List<Payment> findByAppointmentIdOrderByPaidAt(Long appointmentId); List<Payment> findByPaidAtBetween(OffsetDateTime from,OffsetDateTime to); }
