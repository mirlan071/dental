package com.dentalcrm.appointment;
import com.dentalcrm.service.ClinicService; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="appointment_services",uniqueConstraints=@UniqueConstraint(name="uk_appointment_service",columnNames={"appointment_id","service_id"})) @Getter @Setter @NoArgsConstructor
public class AppointmentServiceItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="appointment_id",nullable=false) private Appointment appointment;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_id",nullable=false) private ClinicService service;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
}
