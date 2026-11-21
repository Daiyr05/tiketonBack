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
    private String status;
    private String promoCode;
    private List<ItemResponse> items;
    private LocalDateTime createdAt;
    // Set for PENDING orders: after this moment (UTC) the order is cancelled and its seats are released
    private LocalDateTime paymentDeadline;

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