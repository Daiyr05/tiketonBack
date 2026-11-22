package org.example.lateticket.dto.checkin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendeeResponse {
    private Long ticketId;
    private String ticketCode;
    private Long orderId;
    private String attendeeName;
    private String attendeeEmail;
    private String ticketType;
    private String status;
    private String seatSection;
    private String seatRow;
    private String seatNumber;
    private LocalDateTime checkedInAt;
}
