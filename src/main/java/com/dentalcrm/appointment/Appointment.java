package com.dentalcrm.appointment;
import com.dentalcrm.doctor.Doctor; import com.dentalcrm.patient.Patient; import com.dentalcrm.user.User;
import jakarta.persistence.*; import lombok.*; import java.time.*;
@Entity @Table(name="appointments") @Getter @Setter @NoArgsConstructor
public class Appointment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="patient_id",nullable=false) private Patient patient;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="doctor_id",nullable=false) private Doctor doctor;
 @Column(name="start_time",nullable=false) private OffsetDateTime startTime;
 @Column(name="end_time",nullable=false) private OffsetDateTime endTime;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private AppointmentStatus status=AppointmentStatus.SCHEDULED;
 @Column(columnDefinition="text") private String notes;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private User createdBy;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=Instant.now();}
}
