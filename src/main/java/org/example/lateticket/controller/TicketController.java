package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Ticket;
import org.example.lateticket.entity.User;
import org.example.lateticket.repository.TicketRepository;
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
    private final QrCodeService qrCodeService;
    private final PdfTicketService pdfTicketService;

    @GetMapping("/my")
    public ResponseEntity<List<Ticket>> getMyTickets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketRepository.findByAttendeeId(user.getId()));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Ticket>> getOrderTickets(@PathVariable Long orderId) {
        return ResponseEntity.ok(ticketRepository.findByOrderId(orderId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Ticket>> getEventTickets(@PathVariable Long eventId) {
        return ResponseEntity.ok(ticketRepository.findByEventId(eventId));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        byte[] qrImage = qrCodeService.generateQrCode(ticket.getTicketCode(), 300, 300);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        byte[] pdf = pdfTicketService.generateTicketPdf(ticket);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ticket-" + ticket.getTicketCode() + ".pdf")
                .body(pdf);
    }
}