package org.example.lateticket.dto.checkin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanRequest {
    @NotBlank
    private String ticketCode;

    // Event the staff member is scanning for. Optional; when set, tickets of other events get WRONG_EVENT.
    private Long eventId;
}
