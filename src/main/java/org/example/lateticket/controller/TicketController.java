package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Event;
import org.example.lateticket.entity.Order;
import org.example.lateticket.entity.Ticket;
import org.example.lateticket.entity.User;
import org.example.lateticket.repository.EventRepository;
import org.example.lateticket.repository.OrderRepository;
import org.example.lateticket.repository.TicketRepository;
import org.example.lateticket.service.AccessService;
import org.example.lateticket.service.QrCodeService;
import org.example.lateticket.service.PdfTicketService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final AccessService accessService;
    private final QrCodeService qrCodeService;
    private final PdfTicketService pdfTicketService;

    @GetMapping("/my")
    public ResponseEntity<List<Ticket>> getMyTickets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketRepository.findByAttendeeId(user.getId()));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Ticket>> getOrderTickets(
            @PathVariable Long orderId, @AuthenticationPrincipal User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        accessService.checkCanViewOrder(order, user);
        return ResponseEntity.ok(ticketRepository.findByOrderId(orderId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Ticket>> getEventTickets(
            @PathVariable Long eventId, @AuthenticationPrincipal User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        accessService.checkCanManageEvent(event, user);
        return ResponseEntity.ok(ticketRepository.findByEventId(eventId));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Ticket ticket = findVisibleTicket(id, user);
        byte[] qrImage = qrCodeService.generateQrCode(ticket.getTicketCode(), 300, 300);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Ticket ticket = findVisibleTicket(id, user);
        byte[] pdf = pdfTicketService.generateTicketPdf(ticket);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ticket-" + ticket.getTicketCode() + ".pdf")
                .body(pdf);
    }

    private Ticket findVisibleTicket(Long id, User user) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        accessService.checkCanViewTicket(ticket, user);
        return ticket;
    }
}
