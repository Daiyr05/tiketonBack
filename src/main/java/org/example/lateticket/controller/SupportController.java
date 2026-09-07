package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.SupportCase;
import org.example.lateticket.entity.SupportMessage;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.SupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/cases")
    public ResponseEntity<SupportCase> createCase(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        String category = (String) body.get("category");
        String subject = (String) body.get("subject");
        String message = (String) body.get("message");
        Long eventId = body.get("eventId") != null ? Long.valueOf(body.get("eventId").toString()) : null;
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        Long ticketId = body.get("ticketId") != null ? Long.valueOf(body.get("ticketId").toString()) : null;

        return ResponseEntity.ok(supportService.createCase(user, category, subject, message,
                eventId, orderId, ticketId));
    }

    @PostMapping("/cases/{id}/messages")
    public ResponseEntity<SupportMessage> addMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportService.addMessage(id, user, body.get("message")));
    }

    @PatchMapping("/cases/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        supportService.updateCaseStatus(id, body.get("status"), user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cases/my")
    public ResponseEntity<List<SupportCase>> getMyCases(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportService.getUserCases(user.getId()));
    }

    @GetMapping("/cases/event/{eventId}")
    public ResponseEntity<List<SupportCase>> getEventCases(@PathVariable Long eventId) {
        return ResponseEntity.ok(supportService.getEventCases(eventId));
    }

    @GetMapping("/cases/{id}/messages")
    public ResponseEntity<List<SupportMessage>> getCaseMessages(@PathVariable Long id) {
        return ResponseEntity.ok(supportService.getCaseMessages(id));
    }
}