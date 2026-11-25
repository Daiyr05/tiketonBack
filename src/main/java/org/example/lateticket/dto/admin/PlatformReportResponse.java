package org.example.lateticket.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Platform totals for orders created / refunds issued in [from, to). */
@Data
@Builder
public class PlatformReportResponse {
    private LocalDateTime from;
    private LocalDateTime to;
    private long ordersTotal;
    private long ordersCompleted;
    private long ordersPending;
    private long ordersCancelled;
    private long ordersRefunded;
    private long ticketsSold;
    /** Sum of completed (and later refunded) order totals, incl. service fee */
    private BigDecimal grossRevenue;
    private BigDecimal discounts;
    /** Platform income: service fees of completed orders */
    private BigDecimal serviceFees;
    private long refundsCount;
    private BigDecimal refundsAmount;
    /** grossRevenue - refundsAmount */
    private BigDecimal netRevenue;
}
