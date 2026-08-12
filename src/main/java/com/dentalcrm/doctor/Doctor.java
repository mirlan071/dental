package com.dentalcrm.doctor;
import com.dentalcrm.user.User;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="doctors") @Getter @Setter @NoArgsConstructor
public class Doctor {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false,unique=true) private User user;
 @Column(length=200) private String specialization;
 @Column(length=30) private String phone;
}
