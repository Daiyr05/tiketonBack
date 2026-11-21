package org.example.lateticket.service;

import org.example.lateticket.entity.TicketType;

import java.time.LocalDateTime;

/** Checks shared by seat holds and order creation: can this ticket type be bought right now? */
final class TicketSalesRules {

    private TicketSalesRules() {
    }

    static void assertOnSale(TicketType ticketType) {
        if (ticketType.isHidden()) {
            throw new RuntimeException("Ticket type is not available: " + ticketType.getName());
        }
        LocalDateTime now = LocalDateTime.now();
        if (ticketType.getSalesStart() != null && now.isBefore(ticketType.getSalesStart())) {
            throw new RuntimeException("Sales have not started yet for: " + ticketType.getName());
        }
        if (ticketType.getSalesEnd() != null && now.isAfter(ticketType.getSalesEnd())) {
            throw new RuntimeException("Sales have ended for: " + ticketType.getName());
        }
    }
}
