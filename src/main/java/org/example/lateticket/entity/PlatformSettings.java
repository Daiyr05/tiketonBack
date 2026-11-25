package org.example.lateticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Platform-wide settings. A single row with id = 1. */
@Entity
@Table(name = "platform_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlatformSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** Percentage of the order subtotal (after discounts), e.g. 5.00 = 5%. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal serviceFeePercent;

    /** Fixed amount per paid order, KZT. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceFeeFixed;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
