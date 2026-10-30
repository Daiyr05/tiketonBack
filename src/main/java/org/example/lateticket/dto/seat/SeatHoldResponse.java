package org.example.lateticket.dto.seat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SeatHoldResponse {
    private List<SeatResponse> seats;
    private LocalDateTime heldUntil;
}
