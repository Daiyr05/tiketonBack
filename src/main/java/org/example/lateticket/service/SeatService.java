package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.seat.*;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.SeatStatus;
import org.example.lateticket.repository.SeatRepository;
import org.example.lateticket.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatService {

    static final int HOLD_MINUTES = 10;
    static final int MAX_HELD_SEATS_PER_USER = 10;

    private final SeatRepository seatRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventService eventService;
    private final AuditService auditService;

    @Transactional
    public List<SeatResponse> saveSeatMap(Long eventId, SeatMapRequest request, User organizer) {
        Event event = eventService.getEventEntity(eventId);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }
        ensureNoSeatsTaken(eventId);

        List<Seat> seats = new ArrayList<>();
        Map<Long, Integer> seatsPerTicketType = new HashMap<>();
        Set<String> seen = new HashSet<>();

        for (SeatMapRequest.SectionRequest section : request.getSections()) {
            TicketType ticketType = ticketTypeRepository.findById(section.getTicketTypeId())
                    .orElseThrow(() -> new RuntimeException("Ticket type not found"));
            if (!ticketType.getEvent().getId().equals(eventId)) {
                throw new RuntimeException("Ticket type does not belong to this event");
            }

            for (SeatMapRequest.RowRequest row : resolveRows(section)) {
                for (int n = 1; n <= row.getSeats(); n++) {
                    String key = section.getName() + "|" + row.getLabel() + "|" + n;
                    if (!seen.add(key)) {
                        throw new RuntimeException("Duplicate seat: section " + section.getName()
                                + ", row " + row.getLabel() + ", seat " + n);
                    }
                    seats.add(Seat.builder()
                            .event(event)
                            .ticketType(ticketType)
                            .section(section.getName())
                            .rowLabel(row.getLabel())
                            .seatNumber(String.valueOf(n))
                            .status(SeatStatus.AVAILABLE)
                            .build());
                }
                seatsPerTicketType.merge(ticketType.getId(), row.getSeats(), Integer::sum);
            }
        }

        // Each seat must be sellable, so a ticket type can't have more seats than tickets left
        for (Map.Entry<Long, Integer> entry : seatsPerTicketType.entrySet()) {
            TicketType tt = ticketTypeRepository.findById(entry.getKey()).orElseThrow();
            int available = tt.getQuantity() - tt.getSold();
            if (entry.getValue() > available) {
                throw new RuntimeException("Seat map has " + entry.getValue() + " seats for '" + tt.getName()
                        + "' but only " + available + " tickets are available; increase the ticket type quantity");
            }
        }

        seatRepository.deleteByEventId(eventId);
        seatRepository.flush();
        seatRepository.saveAll(seats);

        event.setAssignedSeating(true);

        auditService.log(eventId, organizer.getId(), "UPDATE", "SeatMap", eventId,
                "Seat map saved: " + seats.size() + " seats");

        return seats.stream().map(s -> toResponse(s, null)).toList();
    }

    @Transactional
    public void deleteSeatMap(Long eventId, User organizer) {
        Event event = eventService.getEventEntity(eventId);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }
        ensureNoSeatsTaken(eventId);
        seatRepository.deleteByEventId(eventId);
        auditService.log(eventId, organizer.getId(), "DELETE", "SeatMap", eventId, "Seat map deleted");
    }

    public List<SeatResponse> getSeatMap(Long eventId, User viewer) {
        return seatRepository.findByEventIdOrderByIdAsc(eventId)
                .stream().map(s -> toResponse(s, viewer)).toList();
    }

    @Transactional
    public SeatHoldResponse holdSeats(Long eventId, SeatHoldRequest request, User user) {
        Set<Long> ids = new LinkedHashSet<>(request.getSeatIds());
        List<Seat> seats = seatRepository.lockByEventIdAndIds(eventId, ids);
        if (seats.size() != ids.size()) {
            throw new RuntimeException("Some seats do not exist for this event");
        }

        long alreadyHeld = seatRepository.countActiveHolds(eventId, user.getId(), LocalDateTime.now());
        long newHolds = seats.stream().filter(s -> !s.isHeldBy(user)).count();
        if (alreadyHeld + newHolds > MAX_HELD_SEATS_PER_USER) {
            throw new RuntimeException("You can hold at most " + MAX_HELD_SEATS_PER_USER + " seats at a time");
        }

        LocalDateTime heldUntil = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
        for (Seat seat : seats) {
            if (!seat.isFree() && !seat.isHeldBy(user)) {
                throw new RuntimeException("Seat is no longer available: " + describe(seat));
            }
            TicketSalesRules.assertOnSale(seat.getTicketType());
            seat.setStatus(SeatStatus.HELD);
            seat.setHeldBy(user);
            seat.setHeldUntil(heldUntil);
        }
        seatRepository.saveAll(seats);

        return SeatHoldResponse.builder()
                .seats(seats.stream().map(s -> toResponse(s, user)).toList())
                .heldUntil(heldUntil)
                .build();
    }

    /** Releases the given seats, or all of the user's holds for the event when seatIds is empty. */
    @Transactional
    public void releaseHolds(Long eventId, List<Long> seatIds, User user) {
        List<Seat> seats = seatRepository.lockHeldBy(eventId, user.getId());
        for (Seat seat : seats) {
            if (seatIds == null || seatIds.isEmpty() || seatIds.contains(seat.getId())) {
                seat.release();
            }
        }
        seatRepository.saveAll(seats);
    }

    /**
     * Converts the attendee's holds into sold seats for the given order item.
     * Must be called inside the order transaction.
     */
    List<Seat> claimHeldSeats(Event event, TicketType ticketType, List<Long> seatIds, int quantity, User attendee) {
        Set<Long> ids = new LinkedHashSet<>(seatIds == null ? List.of() : seatIds);
        if (ids.size() != quantity) {
            throw new RuntimeException("Select exactly " + quantity + " seat(s) for: " + ticketType.getName());
        }
        List<Seat> seats = seatRepository.lockByEventIdAndIds(event.getId(), ids);
        if (seats.size() != ids.size()) {
            throw new RuntimeException("Some seats do not exist for this event");
        }
        for (Seat seat : seats) {
            if (!seat.getTicketType().getId().equals(ticketType.getId())) {
                throw new RuntimeException("Seat " + describe(seat) + " is not a " + ticketType.getName() + " seat");
            }
            if (!seat.isHeldBy(attendee)) {
                throw new RuntimeException("Seat hold expired or not yours: " + describe(seat));
            }
        }
        return seats;
    }

    void markSold(List<Seat> seats, Order order) {
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.SOLD);
            seat.setOrder(order);
            seat.setHeldBy(null);
            seat.setHeldUntil(null);
        }
        seatRepository.saveAll(seats);
    }

    List<Seat> getOrderSeats(Long orderId) {
        return seatRepository.findByOrderId(orderId);
    }

    void releaseOrderSeats(Long orderId) {
        List<Seat> seats = seatRepository.findByOrderId(orderId);
        seats.forEach(Seat::release);
        seatRepository.saveAll(seats);
    }

    boolean hasSeatMap(Long eventId) {
        return seatRepository.existsByEventId(eventId);
    }

    private void ensureNoSeatsTaken(Long eventId) {
        if (seatRepository.existsByEventIdAndStatus(eventId, SeatStatus.SOLD)) {
            throw new RuntimeException("Seat map cannot be changed after seats have been sold");
        }
        boolean activeHolds = seatRepository.findByEventIdOrderByIdAsc(eventId)
                .stream().anyMatch(Seat::isHoldActive);
        if (activeHolds) {
            throw new RuntimeException("Seat map cannot be changed while attendees are holding seats");
        }
    }

    private List<SeatMapRequest.RowRequest> resolveRows(SeatMapRequest.SectionRequest section) {
        if (section.getRows() != null && !section.getRows().isEmpty()) {
            return section.getRows();
        }
        if (section.getRowCount() == null || section.getSeatsPerRow() == null) {
            throw new RuntimeException("Section " + section.getName() + " needs either rows or rowCount + seatsPerRow");
        }
        List<SeatMapRequest.RowRequest> rows = new ArrayList<>();
        for (int r = 1; r <= section.getRowCount(); r++) {
            SeatMapRequest.RowRequest row = new SeatMapRequest.RowRequest();
            row.setLabel(String.valueOf(r));
            row.setSeats(section.getSeatsPerRow());
            rows.add(row);
        }
        return rows;
    }

    private String describe(Seat seat) {
        return "Sector " + seat.getSection() + ", Row " + seat.getRowLabel() + ", Seat " + seat.getSeatNumber();
    }

    private SeatResponse toResponse(Seat seat, User viewer) {
        String status = seat.isFree() ? SeatStatus.AVAILABLE.name() : seat.getStatus().name();
        boolean heldByMe = viewer != null && seat.isHeldBy(viewer);
        return SeatResponse.builder()
                .id(seat.getId())
                .section(seat.getSection())
                .row(seat.getRowLabel())
                .number(seat.getSeatNumber())
                .ticketTypeId(seat.getTicketType().getId())
                .ticketTypeName(seat.getTicketType().getName())
                .price(seat.getTicketType().getPrice())
                .status(status)
                .heldByMe(heldByMe)
                .heldUntil(heldByMe ? seat.getHeldUntil() : null)
                .build();
    }
}
