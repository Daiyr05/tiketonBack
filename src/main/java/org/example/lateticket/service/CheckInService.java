package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.TicketStatus;
import org.example.lateticket.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final TicketRepository ticketRepository;
    private final CheckInRecordRepository checkInRecordRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final AuditService auditService;

    @Transactional
    public Map<String, Object> scanTicket(String ticketCode, User eventAdmin) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Verify staff is assigned to this event
        if (!staffAssignmentRepository.existsByEventIdAndUserId(
                ticket.getEvent().getId(), eventAdmin.getId())) {
            throw new RuntimeException("Not authorized for this event");
        }

        if (ticket.getStatus() == TicketStatus.CHECKED_IN) {
            return Map.of("valid", false, "message", "Ticket already used",
                    "ticketCode", ticketCode, "attendeeName", ticket.getAttendeeName());
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return Map.of("valid", false, "message", "Ticket is cancelled",
                    "ticketCode", ticketCode);
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            return Map.of("valid", false, "message", "Ticket has been refunded",
                    "ticketCode", ticketCode);
        }

        // Valid ticket - check in
        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticketRepository.save(ticket);

        CheckInRecord record = CheckInRecord.builder()
                .ticket(ticket)
                .event(ticket.getEvent())
                .checkedBy(eventAdmin)
                .reversed(false)
                .build();
        checkInRecordRepository.save(record);

        auditService.log(ticket.getEvent().getId(), eventAdmin.getId(), "CHECK_IN",
                "Ticket", ticket.getId(), "Ticket checked in: " + ticket.getAttendeeName());

        return Map.of(
                "valid", true,
                "message", "Check-in successful",
                "ticketCode", ticketCode,
                "attendeeName", ticket.getAttendeeName(),
                "ticketType", ticket.getTicketType().getName(),
                "seatSection", ticket.getSeatSection() != null ? ticket.getSeatSection() : "",
                "seatRow", ticket.getSeatRow() != null ? ticket.getSeatRow() : "",
                "seatNumber", ticket.getSeatNumber() != null ? ticket.getSeatNumber() : ""
        );
    }

    @Transactional
    public Map<String, Object> undoCheckIn(Long ticketId, User eventAdmin) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!staffAssignmentRepository.existsByEventIdAndUserId(
                ticket.getEvent().getId(), eventAdmin.getId())) {
            throw new RuntimeException("Not authorized for this event");
        }

        if (ticket.getStatus() != TicketStatus.CHECKED_IN) {
            throw new RuntimeException("Ticket is not checked in");
        }

        ticket.setStatus(TicketStatus.VALID);
        ticketRepository.save(ticket);

        // Mark latest check-in as reversed
        List<CheckInRecord> records = checkInRecordRepository.findByTicketId(ticketId);
        if (!records.isEmpty()) {
            CheckInRecord latest = records.getLast();
            latest.setReversed(true);
            checkInRecordRepository.save(latest);
        }

        auditService.log(ticket.getEvent().getId(), eventAdmin.getId(), "CHECK_IN_REVERSE",
                "Ticket", ticket.getId(), "Check-in reversed: " + ticket.getAttendeeName());

        return Map.of("message", "Check-in reversed", "ticketCode", ticket.getTicketCode());
    }

    public Map<String, Object> getEventCheckInStats(Long eventId) {
        long totalCheckedIn = checkInRecordRepository.countByEventIdAndReversedFalse(eventId);
        long totalTickets = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.VALID)
                + ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.CHECKED_IN);

        return Map.of(
                "totalRegistered", totalTickets,
                "checkedIn", totalCheckedIn,
                "notCheckedIn", totalTickets - totalCheckedIn
        );
    }
}