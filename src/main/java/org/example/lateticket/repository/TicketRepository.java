package org.example.lateticket.repository;

import org.example.lateticket.entity.Ticket;
import org.example.lateticket.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByOrderId(Long orderId);
    List<Ticket> findByEventId(Long eventId);
    List<Ticket> findByAttendeeId(Long attendeeId);
    long countByEventIdAndStatus(Long eventId, TicketStatus status);
}