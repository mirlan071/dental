package com.dentalcrm.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "app_data_seeds")
@Getter
@NoArgsConstructor
public class DemoDataSeedMarker {
    @Id
    @Column(name = "seed_key", nullable = false, length = 100)
    private String seedKey;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;

    public DemoDataSeedMarker(String seedKey) {
        this.seedKey = seedKey;
        this.completedAt = Instant.now();
    }
}
