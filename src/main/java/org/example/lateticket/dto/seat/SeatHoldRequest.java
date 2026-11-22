package org.example.lateticket.dto.seat;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SeatHoldRequest {
    @NotEmpty
    private List<Long> seatIds;
}
