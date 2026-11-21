package org.example.lateticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.order.*;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.createOrder(request, user));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> completePayment(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.completePayment(id, user));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<OrderResponse> refundOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.refundOrder(id, user, body.get("reason")));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.cancelOrder(id, user));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getAttendeeOrders(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getOrder(id, user));
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<OrderResponse>> getEventOrders(
            @PathVariable Long eventId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getEventOrders(eventId, user));
    }
}