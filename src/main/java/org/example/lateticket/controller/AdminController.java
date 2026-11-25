package org.example.lateticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.admin.*;
import org.example.lateticket.dto.event.EventResponse;
import org.example.lateticket.dto.order.OrderResponse;
import org.example.lateticket.dto.user.UserResponse;
import org.example.lateticket.entity.SupportCase;
import org.example.lateticket.entity.User;
import org.example.lateticket.repository.EventRepository;
import org.example.lateticket.repository.UserRepository;
import org.example.lateticket.service.AdminService;
import org.example.lateticket.service.EventService;
import org.example.lateticket.service.OrderService;
import org.example.lateticket.service.SupportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final OrderService orderService;
    private final SupportService supportService;
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(UserResponse::from).toList());
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getEvents() {
        return ResponseEntity.ok(eventRepository.findAll().stream().map(eventService::toResponse).toList());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /** Platform support cases (no event attached): account, activation, payment, technical. */
    @GetMapping("/support-cases")
    public ResponseEntity<List<SupportCase>> getSupportCases(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(supportService.getPlatformCases(status));
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<Map<String, String>> suspendUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setSuspended(true);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User suspended"));
    }

    @PostMapping("/users/{id}/unsuspend")
    public ResponseEntity<Map<String, String>> unsuspendUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setSuspended(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User unsuspended"));
    }

    /** Cancels the whole event and stops sales. */
    @PostMapping("/events/{id}/suspend")
    public ResponseEntity<Map<String, String>> suspendEvent(
            @PathVariable Long id, @AuthenticationPrincipal User admin) {
        adminService.suspendEvent(id, admin);
        return ResponseEntity.ok(Map.of("message", "Event suspended and sales stopped"));
    }

    /** Stops paid ticket sales only; body {"reason": "..."} is optional. */
    @PostMapping("/events/{id}/suspend-sales")
    public ResponseEntity<Map<String, String>> suspendSales(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        adminService.suspendPaidSales(id, body != null ? body.get("reason") : null, admin);
        return ResponseEntity.ok(Map.of("message", "Paid sales suspended"));
    }

    @PostMapping("/events/{id}/resume-sales")
    public ResponseEntity<Map<String, String>> resumeSales(
            @PathVariable Long id, @AuthenticationPrincipal User admin) {
        adminService.resumePaidSales(id, admin);
        return ResponseEntity.ok(Map.of("message", "Paid sales resumed"));
    }

    /** Paid-sales activation history: organizer activations, admin suspensions and resumptions. */
    @GetMapping("/activations")
    public ResponseEntity<List<ActivationRecordResponse>> getActivations() {
        return ResponseEntity.ok(adminService.getActivationRecords());
    }

    @GetMapping("/reports")
    public ResponseEntity<List<EventReportResponse>> getReports(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.getReports(status));
    }

    @PatchMapping("/reports/{id}")
    public ResponseEntity<EventReportResponse> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportUpdateRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.updateReport(id, request, admin));
    }

    /** Totals for a period; defaults to the last 30 days. Dates are ISO, e.g. 2026-11-01T00:00:00 */
    @GetMapping("/platform-report")
    public ResponseEntity<PlatformReportResponse> getPlatformReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(adminService.getPlatformReport(from, to));
    }

    @GetMapping("/settings")
    public ResponseEntity<PlatformSettingsDto> getSettings() {
        return ResponseEntity.ok(adminService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<PlatformSettingsDto> updateSettings(
            @Valid @RequestBody PlatformSettingsDto request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.updateSettings(request, admin));
    }
}
