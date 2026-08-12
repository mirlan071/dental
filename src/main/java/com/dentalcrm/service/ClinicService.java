package com.dentalcrm.service;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="services") @Getter @Setter @NoArgsConstructor
public class ClinicService {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=200) private String name;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
 @Column(name="duration_minutes",nullable=false) private Integer durationMinutes;
 @Column(nullable=false) private boolean active=true;
}
