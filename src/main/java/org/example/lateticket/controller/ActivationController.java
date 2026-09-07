package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.ActivationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class ActivationController {

    private final ActivationService activationService;

    @GetMapping("/{id}/activation-checklist")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Map<String, Object>> getChecklist(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(activationService.getActivationChecklist(id, user));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Map<String, Object>> activate(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(activationService.activatePaidSales(id, user));
    }
}