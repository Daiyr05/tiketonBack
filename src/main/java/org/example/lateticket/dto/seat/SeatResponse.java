package org.example.lateticket.dto.seat;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SeatResponse {
    private Long id;
    private String section;
    private String row;
    private String number;
    private Long ticketTypeId;
    private String ticketTypeName;
    private BigDecimal price;
    /** Reserved for people with disabilities (SRS 4.3.1) */
    private boolean accessible;
    // AVAILABLE, HELD or SOLD (expired holds are reported as AVAILABLE)
    private String status;
    private boolean heldByMe;
    private LocalDateTime heldUntil;
}
