package org.example.lateticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.checkin.*;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.CheckInService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Check-in API for the mobile verification app.
 * Access: staff assigned to the event, the event's organizer, or a platform admin.
 */
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /** Scan a QR code. Always 200 with a verdict; 400 only when the caller has no access to the event. */
    @PostMapping("/scan")
    public ResponseEntity<ScanResponse> scanTicket(
            @Valid @RequestBody ScanRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(checkInService.scanTicket(request, user));
    }

    /** Manual check-in from the attendee list. */
    @PostMapping("/tickets/{ticketId}")
    public ResponseEntity<ScanResponse> checkInById(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(checkInService.checkInById(ticketId, user));
    }

    @PostMapping("/undo/{ticketId}")
    public ResponseEntity<Map<String, Object>> undoCheckIn(
            @PathVariable Long ticketId,
            @RequestBody(required = false) UndoRequest body,
            @AuthenticationPrincipal User user) {
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(checkInService.undoCheckIn(ticketId, reason, user));
    }

    /** Attendee search: name, email, ticket code or order id. Empty q returns everyone. */
    @GetMapping("/events/{eventId}/attendees")
    public ResponseEntity<List<AttendeeResponse>> searchAttendees(
            @PathVariable Long eventId,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(checkInService.searchAttendees(eventId, q, user));
    }

    @GetMapping("/stats/{eventId}")
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(checkInService.getEventCheckInStats(eventId, user));
    }
}
