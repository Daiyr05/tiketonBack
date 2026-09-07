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

        // For assigned seating
        private String seatSection;
        private String seatRow;
        private String seatNumber;
    }
}