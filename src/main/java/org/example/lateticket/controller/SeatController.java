package org.example.lateticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.seat.*;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeatMap(
            @PathVariable Long eventId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(seatService.getSeatMap(eventId, user));
    }

    @PutMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<SeatResponse>> saveSeatMap(
            @PathVariable Long eventId,
            @Valid @RequestBody SeatMapRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(seatService.saveSeatMap(eventId, request, user));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteSeatMap(
            @PathVariable Long eventId, @AuthenticationPrincipal User user) {
        seatService.deleteSeatMap(eventId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hold")
    public ResponseEntity<SeatHoldResponse> holdSeats(
            @PathVariable Long eventId,
            @Valid @RequestBody SeatHoldRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(seatService.holdSeats(eventId, request, user));
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseHolds(
            @PathVariable Long eventId,
            @RequestBody(required = false) SeatHoldRequest request,
            @AuthenticationPrincipal User user) {
        seatService.releaseHolds(eventId, request != null ? request.getSeatIds() : null, user);
        return ResponseEntity.ok().build();
    }
}
