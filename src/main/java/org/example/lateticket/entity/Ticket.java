package org.example.lateticket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.example.lateticket.entity.enums.TicketStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ticketCode;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"items", "attendee", "event"})
    private Order order;

    @ManyToOne
    @JoinColumn(name = "ticket_type_id", nullable = false)
    @JsonIgnoreProperties({"event"})
    private TicketType ticketType;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnoreProperties({"description", "organizer"})
    private Event event;

    @ManyToOne
    @JoinColumn(name = "attendee_id", nullable = false)
    @JsonIgnoreProperties({"password", "verificationToken", "resetPasswordToken", "resetPasswordExpiry"})
    private User attendee;

    private String attendeeName;
    private String attendeeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    // Assigned seating
    private String seatSection;
    private String seatRow;
    private String seatNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}