package com.dentalcrm.payment;
import com.dentalcrm.appointment.Appointment; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.*;
@Entity @Table(name="payments") @Getter @Setter @NoArgsConstructor
public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="appointment_id",nullable=false) private Appointment appointment;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Enumerated(EnumType.STRING) @Column(name="payment_method",nullable=false,length=20) private PaymentMethod paymentMethod;
 @Column(name="paid_at",nullable=false) private OffsetDateTime paidAt;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=Instant.now();}
}
