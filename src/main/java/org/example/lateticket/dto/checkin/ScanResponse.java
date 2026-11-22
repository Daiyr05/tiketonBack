package org.example.lateticket.dto.checkin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanResponse {
    private ScanVerdict verdict;
    private boolean valid;
    private String message;

    private String ticketCode;
    private Long ticketId;
    private Long orderId;
    private Long eventId;
    private String eventTitle;

    private String attendeeName;
    private String attendeeEmail;
    private String ticketType;
    private String status;

    private String seatSection;
    private String seatRow;
    private String seatNumber;

    // Set when the ticket has an active check-in (verdict OK or ALREADY_USED)
    private LocalDateTime checkedInAt;
    private String checkedInBy;

    // Set only for verdict PROMO_CODE
    private PromoInfo promo;

    @Data
    @Builder
    public static class PromoInfo {
        private String code;
        private String campaignName;
        private String discountType;
        private BigDecimal discountValue;
        private Long eventId;
        private boolean active;
    }
}
