package com.dentalcrm.settings;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "clinic_settings")
@Getter
@Setter
@NoArgsConstructor
public class ClinicSettings {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "workday_start", nullable = false)
    private LocalTime workdayStart;

    @Column(name = "workday_end", nullable = false)
    private LocalTime workdayEnd;

    @Column(nullable = false, length = 100)
    private String timezone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
