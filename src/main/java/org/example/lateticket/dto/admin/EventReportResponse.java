package org.example.lateticket.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventReportResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String eventStatus;
    private Long reporterId;
    private String reporterEmail;
    private String reason;
    /** OPEN, RESOLVED or DISMISSED */
    private String status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
