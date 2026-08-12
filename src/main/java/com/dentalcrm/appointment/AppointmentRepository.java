package com.dentalcrm.appointment;
import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.OffsetDateTime; import java.util.*;
public interface AppointmentRepository extends JpaRepository<Appointment,Long>{
 @Query("select (count(a)>0) from Appointment a where a.doctor.id=:doctorId and a.status not in (com.dentalcrm.appointment.AppointmentStatus.CANCELLED,com.dentalcrm.appointment.AppointmentStatus.NO_SHOW) and a.startTime<:endTime and a.endTime>:startTime and (:excludeId is null or a.id<>:excludeId)")
 boolean hasConflict(@Param("doctorId") Long doctorId,@Param("startTime") OffsetDateTime startTime,@Param("endTime") OffsetDateTime endTime,@Param("excludeId") Long excludeId);
 List<Appointment> findByDoctorIdAndStartTimeBetweenOrderByStartTime(Long doctorId,OffsetDateTime from,OffsetDateTime to);
 List<Appointment> findByStartTimeBetweenOrderByStartTime(OffsetDateTime from,OffsetDateTime to);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from Appointment a where a.id=:id") Optional<Appointment> findByIdForUpdate(@Param("id") Long id);
}
