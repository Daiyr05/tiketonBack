package org.example.lateticket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.lateticket.entity.enums.EventStatus;
import org.example.lateticket.entity.enums.EventVisibility;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organizer_id", nullable = false)
    @JsonIgnoreProperties({"password", "verificationToken", "resetPasswordToken", "resetPasswordExpiry"})
    private User organizer;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private String imageUrl;

    // Venue info
    private String venueName;
    private String venueAddress;
    private String city;

    private LocalDateTime eventDate;
    private LocalDateTime eventEndDate;

    private LocalDateTime registrationOpenDate;
    private LocalDateTime registrationCloseDate;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventVisibility visibility;

    /** Refund policy shown to attendees (SRS 4.9), free text */
    @Column(columnDefinition = "TEXT")
    private String refundPolicy;

    private boolean paidSalesActivated;
    private boolean assignedSeating;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}