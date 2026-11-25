package org.example.lateticket.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** One paid-sales activation change (activated by organizer, suspended/resumed by admin). */
@Data
@Builder
public class ActivationRecordResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long organizerId;
    private String organizerEmail;
    /** ACTIVATE_PAID_SALES, SUSPEND_PAID_SALES or RESUME_PAID_SALES */
    private String action;
    private Long performedBy;
    private String description;
    /** Current state of the event */
    private boolean paidSalesActivated;
    private LocalDateTime timestamp;
}
