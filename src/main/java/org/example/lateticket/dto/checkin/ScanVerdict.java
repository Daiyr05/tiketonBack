package org.example.lateticket.dto.checkin;

/** Machine-readable outcome of a ticket scan. Clients should branch on this, not on the message text. */
public enum ScanVerdict {
    OK,            // ticket valid, check-in recorded
    ALREADY_USED,  // ticket was already checked in (see firstCheckedInAt)
    CANCELLED,     // ticket cancelled
    REFUNDED,      // ticket refunded
    WRONG_EVENT,   // ticket belongs to another event than the one being scanned
    NOT_FOUND,     // no ticket with this code
    PROMO_CODE     // the scanned code is a promo/campaign code, not a ticket
}
