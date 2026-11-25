package org.example.lateticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.lateticket.entity.enums.ReportStatus;

import java.time.LocalDateTime;

/** A user's complaint about an event (SRS 4.12). */
@Entity
@Table(name = "event_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
