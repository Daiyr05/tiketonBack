package org.example.lateticket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.lateticket.entity.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotional_campaigns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionalCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnoreProperties({"description", "organizer"})
    private Event event;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    private Integer maxRedemptions;
    private Integer currentRedemptions;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Filled in for the organizer's campaign list; not a column. */
    @Transient
    private String promoCode;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentRedemptions == null) currentRedemptions = 0;
    }
}