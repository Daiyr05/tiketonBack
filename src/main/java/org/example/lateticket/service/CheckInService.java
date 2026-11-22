package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.checkin.*;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.TicketStatus;
import org.example.lateticket.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final TicketRepository ticketRepository;
    private final CheckInRecordRepository checkInRecordRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final EventRepository eventRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final AuditService auditService;

    /**
     * Scans a QR code. Never throws for a bad ticket: every outcome comes back as a verdict,
     * so the mobile app can branch on {@link ScanVerdict} instead of message text.
     * Throws only when the staff member has no access to the event being scanned.
     */
    @Transactional
    public ScanResponse scanTicket(ScanRequest request, User staff) {
        String code = request.getTicketCode() == null ? "" : request.getTicketCode().trim();
        Long scanEventId = request.getEventId();

        if (scanEventId != null) {
            requireEventAccess(scanEventId, staff);
        }

        Optional<Ticket> found = ticketRepository.findByTicketCode(code);
        if (found.isEmpty()) {
            return promoCodeRepository.findByCode(code)
                    .map(promo -> promoResponse(code, promo))
                    .orElseGet(() -> ScanResponse.builder()
                            .verdict(ScanVerdict.NOT_FOUND)
                            .valid(false)
                            .message("Ticket not found")
                            .ticketCode(code)
                            .build());
        }
        Ticket ticket = found.get();

        if (scanEventId == null) {
            requireEventAccess(ticket.getEvent().getId(), staff);
        } else if (!ticket.getEvent().getId().equals(scanEventId)) {
            return base(ticket, ScanVerdict.WRONG_EVENT, "Ticket belongs to another event").build();
        }

        return checkIn(ticket, staff);
    }

    /** Manual check-in from the attendee list (no QR). */
    @Transactional
    public ScanResponse checkInById(Long ticketId, User staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        requireEventAccess(ticket.getEvent().getId(), staff);
        return checkIn(ticket, staff);
    }

    private ScanResponse checkIn(Ticket ticket, User staff) {
        switch (ticket.getStatus()) {
            case CHECKED_IN -> {
                return withCheckInInfo(ticket, base(ticket, ScanVerdict.ALREADY_USED, "Ticket already used")).build();
            }
            case CANCELLED -> {
                return base(ticket, ScanVerdict.CANCELLED, "Ticket is cancelled").build();
            }
            case REFUNDED -> {
                return base(ticket, ScanVerdict.REFUNDED, "Ticket has been refunded").build();
            }
            default -> { }
        }

        ticket.setStatus(TicketStatus.CHECKED_IN);
        ticketRepository.save(ticket);

        CheckInRecord record = CheckInRecord.builder()
                .ticket(ticket)
                .event(ticket.getEvent())
                .checkedBy(staff)
                .reversed(false)
                .build();
        record = checkInRecordRepository.save(record);

        auditService.log(ticket.getEvent().getId(), staff.getId(), "CHECK_IN",
                "Ticket", ticket.getId(), "Ticket checked in: " + ticket.getAttendeeName());

        return base(ticket, ScanVerdict.OK, "Check-in successful")
                .valid(true)
                .status(TicketStatus.CHECKED_IN.name())
                .checkedInAt(record.getTimestamp())
                .checkedInBy(fullName(staff))
                .build();
    }

    @Transactional
    public Map<String, Object> undoCheckIn(Long ticketId, String reason, User staff) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        requireEventAccess(ticket.getEvent().getId(), staff);

        if (ticket.getStatus() != TicketStatus.CHECKED_IN) {
            throw new RuntimeException("Ticket is not checked in");
        }

        ticket.setStatus(TicketStatus.VALID);
        ticketRepository.save(ticket);

        // Mark the active check-in as reversed
        checkInRecordRepository.findFirstByTicketIdAndReversedFalseOrderByTimestampAsc(ticketId)
                .ifPresent(record -> {
                    record.setReversed(true);
                    record.setReversalReason(reason);
                    checkInRecordRepository.save(record);
                });

        String description = "Check-in reversed: " + ticket.getAttendeeName()
                + (reason != null && !reason.isBlank() ? " (reason: " + reason + ")" : "");
        auditService.log(ticket.getEvent().getId(), staff.getId(), "CHECK_IN_REVERSE",
                "Ticket", ticket.getId(), description);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Check-in reversed");
        result.put("ticketId", ticket.getId());
        result.put("ticketCode", ticket.getTicketCode());
        result.put("status", ticket.getStatus().name());
        return result;
    }

    /**
     * Attendee list for the check-in app. Matches name, email, ticket code or order id (case-insensitive substring).
     * An empty query returns every ticket of the event.
     */
    public List<AttendeeResponse> searchAttendees(Long eventId, String query, User staff) {
        requireEventAccess(eventId, staff);
        String q = query == null ? "" : query.trim().toLowerCase();

        Map<Long, CheckInRecord> activeCheckIns = new HashMap<>();
        for (CheckInRecord r : checkInRecordRepository.findByEventId(eventId)) {
            if (!r.isReversed()) activeCheckIns.putIfAbsent(r.getTicket().getId(), r);
        }

        return ticketRepository.findByEventId(eventId).stream()
                .filter(t -> q.isEmpty() || matches(t, q))
                .sorted(Comparator.comparing(Ticket::getAttendeeName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(t -> {
                    CheckInRecord r = activeCheckIns.get(t.getId());
                    return AttendeeResponse.builder()
                            .ticketId(t.getId())
                            .ticketCode(t.getTicketCode())
                            .orderId(t.getOrder().getId())
                            .attendeeName(t.getAttendeeName())
                            .attendeeEmail(t.getAttendeeEmail())
                            .ticketType(t.getTicketType().getName())
                            .status(t.getStatus().name())
                            .seatSection(t.getSeatSection())
                            .seatRow(t.getSeatRow())
                            .seatNumber(t.getSeatNumber())
                            .checkedInAt(r != null ? r.getTimestamp() : null)
                            .build();
                })
                .toList();
    }

    public Map<String, Object> getEventCheckInStats(Long eventId, User staff) {
        requireEventAccess(eventId, staff);

        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        long valid = tickets.stream().filter(t -> t.getStatus() == TicketStatus.VALID).count();
        long checkedIn = tickets.stream().filter(t -> t.getStatus() == TicketStatus.CHECKED_IN).count();
        long cancelled = tickets.stream().filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();
        long refunded = tickets.stream().filter(t -> t.getStatus() == TicketStatus.REFUNDED).count();
        long totalRegistered = valid + checkedIn;

        // Per ticket type
        Map<Long, Map<String, Object>> byType = new LinkedHashMap<>();
        for (Ticket t : tickets) {
            if (t.getStatus() == TicketStatus.CANCELLED || t.getStatus() == TicketStatus.REFUNDED) continue;
            Map<String, Object> row = byType.computeIfAbsent(t.getTicketType().getId(), id -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ticketTypeId", id);
                m.put("name", t.getTicketType().getName());
                m.put("registered", 0L);
                m.put("checkedIn", 0L);
                return m;
            });
            row.put("registered", (Long) row.get("registered") + 1);
            if (t.getStatus() == TicketStatus.CHECKED_IN) {
                row.put("checkedIn", (Long) row.get("checkedIn") + 1);
            }
        }

        List<Map<String, Object>> recent = checkInRecordRepository
                .findTop20ByEventIdAndReversedFalseOrderByTimestampDesc(eventId).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ticketId", r.getTicket().getId());
                    m.put("attendeeName", r.getTicket().getAttendeeName());
                    m.put("ticketType", r.getTicket().getTicketType().getName());
                    m.put("checkedInAt", r.getTimestamp());
                    m.put("checkedBy", r.getCheckedBy() != null ? fullName(r.getCheckedBy()) : null);
                    return m;
                })
                .toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRegistered", totalRegistered);
        stats.put("checkedIn", checkedIn);
        stats.put("notCheckedIn", valid);
        stats.put("cancelled", cancelled);
        stats.put("refunded", refunded);
        stats.put("reversed", checkInRecordRepository.countByEventIdAndReversedTrue(eventId));
        stats.put("byTicketType", new ArrayList<>(byType.values()));
        stats.put("recentCheckIns", recent);
        return stats;
    }

    // ---- helpers ----

    /** Staff assigned to the event, the event's organizer, or a platform admin. */
    private void requireEventAccess(Long eventId, User user) {
        if (user.getRole() == org.example.lateticket.entity.enums.Role.PLATFORM_ADMIN) return;
        if (staffAssignmentRepository.existsByEventIdAndUserId(eventId, user.getId())) return;
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (event.getOrganizer().getId().equals(user.getId())) return;
        throw new RuntimeException("Not authorized for this event");
    }

    private ScanResponse.ScanResponseBuilder base(Ticket ticket, ScanVerdict verdict, String message) {
        return ScanResponse.builder()
                .verdict(verdict)
                .valid(false)
                .message(message)
                .ticketCode(ticket.getTicketCode())
                .ticketId(ticket.getId())
                .orderId(ticket.getOrder().getId())
                .eventId(ticket.getEvent().getId())
                .eventTitle(ticket.getEvent().getTitle())
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .ticketType(ticket.getTicketType().getName())
                .status(ticket.getStatus().name())
                .seatSection(ticket.getSeatSection())
                .seatRow(ticket.getSeatRow())
                .seatNumber(ticket.getSeatNumber());
    }

    private ScanResponse.ScanResponseBuilder withCheckInInfo(Ticket ticket, ScanResponse.ScanResponseBuilder b) {
        return checkInRecordRepository.findFirstByTicketIdAndReversedFalseOrderByTimestampAsc(ticket.getId())
                .map(r -> b.checkedInAt(r.getTimestamp())
                        .checkedInBy(r.getCheckedBy() != null ? fullName(r.getCheckedBy()) : null))
                .orElse(b);
    }

    private ScanResponse promoResponse(String code, PromoCode promo) {
        PromotionalCampaign c = promo.getCampaign();
        return ScanResponse.builder()
                .verdict(ScanVerdict.PROMO_CODE)
                .valid(false)
                .message("This is a promo code, not a ticket")
                .ticketCode(code)
                .eventId(c.getEvent().getId())
                .eventTitle(c.getEvent().getTitle())
                .promo(ScanResponse.PromoInfo.builder()
                        .code(promo.getCode())
                        .campaignName(c.getName())
                        .discountType(c.getDiscountType().name())
                        .discountValue(c.getDiscountValue())
                        .eventId(c.getEvent().getId())
                        .active(c.isActive())
                        .build())
                .build();
    }

    private boolean matches(Ticket t, String q) {
        return contains(t.getAttendeeName(), q)
                || contains(t.getAttendeeEmail(), q)
                || contains(t.getTicketCode(), q)
                || String.valueOf(t.getOrder().getId()).equals(q);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private String fullName(User u) {
        return ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                + (u.getLastName() != null ? u.getLastName() : "")).trim();
    }
}
