package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.AuditLog;
import org.example.lateticket.service.AnalyticsService;
import org.example.lateticket.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuditService auditService;

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Map<String, Object>> getEventAnalytics(@PathVariable Long eventId) {
        return ResponseEntity.ok(analyticsService.getEventAnalytics(eventId));
    }

    @GetMapping("/events/{eventId}/timeline")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<AuditLog>> getEventTimeline(@PathVariable Long eventId) {
        return ResponseEntity.ok(auditService.getEventTimeline(eventId));
    }
}