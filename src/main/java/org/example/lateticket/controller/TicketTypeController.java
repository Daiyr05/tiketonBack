package org.example.lateticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.ticket.TicketTypeRequest;
import org.example.lateticket.entity.TicketType;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.TicketTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @GetMapping("/events/{eventId}/ticket-types")
    public ResponseEntity<List<TicketType>> getTicketTypes(@PathVariable Long eventId) {
        return ResponseEntity.ok(ticketTypeService.getVisibleTicketTypes(eventId));
    }

    @GetMapping("/events/{eventId}/ticket-types/all")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<TicketType>> getAllTicketTypes(@PathVariable Long eventId) {
        return ResponseEntity.ok(ticketTypeService.getTicketTypes(eventId));
    }

    @PostMapping("/events/{eventId}/ticket-types")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<TicketType> createTicketType(
            @PathVariable Long eventId,
            @Valid @RequestBody TicketTypeRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketTypeService.createTicketType(eventId, request, user));
    }

    @PutMapping("/ticket-types/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<TicketType> updateTicketType(
            @PathVariable Long id,
            @Valid @RequestBody TicketTypeRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketTypeService.updateTicketType(id, request, user));
    }

    @PostMapping("/ticket-types/{id}/hide")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> hideTicketType(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        ticketTypeService.hideTicketType(id, user);
        return ResponseEntity.ok().build();
    }
}