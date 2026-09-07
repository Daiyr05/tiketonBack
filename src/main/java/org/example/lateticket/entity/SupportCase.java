package org.example.lateticket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.example.lateticket.entity.enums.SupportCaseStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "verificationToken", "resetPasswordToken", "resetPasswordExpiry"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @JsonIgnoreProperties({"description", "organizer"})
    private Event event;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnoreProperties({"items", "attendee", "event"})
    private Order order;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    @JsonIgnoreProperties({"order", "event", "attendee"})
    private Ticket ticket;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportCaseStatus status;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    @JsonIgnoreProperties({"password", "verificationToken", "resetPasswordToken", "resetPasswordExpiry"})
    private User assignedTo;

    @OneToMany(mappedBy = "supportCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<SupportMessage> messages = new ArrayList<>();

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