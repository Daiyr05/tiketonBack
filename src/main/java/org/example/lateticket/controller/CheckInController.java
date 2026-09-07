package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.CheckInService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanTicket(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(checkInService.scanTicket(body.get("ticketCode"), user));
    }

    @PostMapping("/undo/{ticketId}")
    public ResponseEntity<Map<String, Object>> undoCheckIn(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(checkInService.undoCheckIn(ticketId, user));
    }

    @GetMapping("/stats/{eventId}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long eventId) {
        return ResponseEntity.ok(checkInService.getEventCheckInStats(eventId));
    }
}