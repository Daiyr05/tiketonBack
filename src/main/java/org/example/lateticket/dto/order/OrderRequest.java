package org.example.lateticket.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotNull
    private Long eventId;

    private String attendeeFirstName;
    private String attendeeLastName;
    private String attendeeEmail;

    private String promoCode;

    @NotNull
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private Long ticketTypeId;

        @NotNull
        private Integer quantity;

        // Seat map: ids of seats held via POST /api/events/{eventId}/seats/hold (one per ticket)
        private List<Long> seatIds;

        // Free-text seat preference, used only when the event has no seat map
        private String seatSection;
        private String seatRow;
        private String seatNumber;
    }
}