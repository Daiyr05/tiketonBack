package org.example.lateticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.event.*;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/public")
    public ResponseEntity<List<EventResponse>> getPublicEvents() {
        return ResponseEntity.ok(eventService.getPublicEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<EventResponse>> getMyEvents(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventService.getOrganizerEvents(user.getId()));
    }

    @GetMapping("/my/status/{status}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<EventResponse>> getMyEventsByStatus(
            @AuthenticationPrincipal User user, @PathVariable String status) {
        return ResponseEntity.ok(eventService.getOrganizerEventsByStatus(user.getId(), status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventService.createEvent(request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, user));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventService.publishEvent(id, user));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> unpublishEvent(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventService.unpublishEvent(id, user));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventService.cancelEvent(id, user));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> duplicateEvent(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(eventService.duplicateEvent(id, user));
    }
}