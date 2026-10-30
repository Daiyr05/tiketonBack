package org.example.lateticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.lateticket.entity.enums.SeatStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    @Column(nullable = false)
    private String section;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    // Temporary hold while the attendee is checking out
    @ManyToOne
    @JoinColumn(name = "held_by")
    private User heldBy;

    private LocalDateTime heldUntil;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    public boolean isHoldActive() {
        return status == SeatStatus.HELD && heldUntil != null && heldUntil.isAfter(LocalDateTime.now());
    }

    public boolean isFree() {
        return status == SeatStatus.AVAILABLE || (status == SeatStatus.HELD && !isHoldActive());
    }

    public boolean isHeldBy(User user) {
        return isHoldActive() && heldBy != null && heldBy.getId().equals(user.getId());
    }

    public void release() {
        status = SeatStatus.AVAILABLE;
        heldBy = null;
        heldUntil = null;
        order = null;
    }
}
