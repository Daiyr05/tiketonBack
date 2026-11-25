package org.example.lateticket.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long attendeeId;
    private String attendeeName;
    private String attendeeEmail;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    /** Platform service fee, already included in totalAmount */
    private BigDecimal serviceFee;
    /** Ticket price total before discount and fee */
    private BigDecimal subtotal;
    private String status;
    private String promoCode;
    private List<ItemResponse> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class ItemResponse {
        private Long id;
        private String ticketTypeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private List<String> seats;
    }
}