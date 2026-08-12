package com.dentalcrm.appointment;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface AppointmentServiceItemRepository extends JpaRepository<AppointmentServiceItem,Long>{ List<AppointmentServiceItem> findByAppointmentIdOrderById(Long appointmentId); default List<AppointmentServiceItem> findByAppointmentId(Long appointmentId){return findByAppointmentIdOrderById(appointmentId);} boolean existsByAppointmentId(Long appointmentId); }
