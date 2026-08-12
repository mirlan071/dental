package com.dentalcrm.patient;
import jakarta.persistence.*; import lombok.*; import java.time.*;
@Entity @Table(name="patients") @Getter @Setter @NoArgsConstructor
public class Patient {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="full_name",nullable=false,length=200) private String fullName;
 @Column(nullable=false,length=30) private String phone;
 @Column(name="birth_date") private LocalDate birthDate;
 @Column(columnDefinition="text") private String notes;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=Instant.now();}
}
