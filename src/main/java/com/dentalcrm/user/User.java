package com.dentalcrm.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=100) private String username;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Column(name="full_name", nullable=false, length=200) private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Role role;
    @Column(nullable=false) private boolean active = true;
    @Column(name="auth_version", nullable=false) private long authVersion;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @PrePersist void onCreate(){ if(createdAt == null) createdAt=Instant.now(); }
}
