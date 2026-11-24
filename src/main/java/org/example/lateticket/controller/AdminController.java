package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Event;
import org.example.lateticket.entity.User;
import org.example.lateticket.entity.enums.EventStatus;
import org.example.lateticket.repository.*;
import org.example.lateticket.service.SupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SupportService supportService;

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEvents() {
        return ResponseEntity.ok(eventRepository.findAll());
    }

    /** Platform support cases (no event attached): account, activation, payment, technical. */
    @GetMapping("/support-cases")
    public ResponseEntity<?> getSupportCases(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(supportService.getPlatformCases(status));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
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

    @PostMapping("/events/{id}/suspend")
    public ResponseEntity<Map<String, String>> suspendEvent(@PathVariable Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.setStatus(EventStatus.CANCELLED);
        event.setPaidSalesActivated(false);
        eventRepository.save(event);
        return ResponseEntity.ok(Map.of("message", "Event suspended and sales stopped"));
    }
}