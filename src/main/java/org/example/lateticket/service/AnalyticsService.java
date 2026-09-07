package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.enums.OrderStatus;
import org.example.lateticket.entity.enums.TicketStatus;
import org.example.lateticket.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final CheckInRecordRepository checkInRecordRepository;
    private final PromotionalCampaignRepository campaignRepository;

    public Map<String, Object> getEventAnalytics(Long eventId) {
        Map<String, Object> analytics = new HashMap<>();

        var ticketTypes = ticketTypeRepository.findByEventId(eventId);
        int totalCapacity = ticketTypes.stream().mapToInt(tt -> tt.getQuantity()).sum();
        int totalSold = ticketTypes.stream().mapToInt(tt -> tt.getSold()).sum();

        analytics.put("totalCapacity", totalCapacity);
        analytics.put("ticketsSold", totalSold);
        analytics.put("ticketsRemaining", totalCapacity - totalSold);
        analytics.put("percentageSold", totalCapacity > 0 ?
                (totalSold * 100.0 / totalCapacity) : 0);

        // Revenue
        var completedOrders = orderRepository.findByEventIdAndStatus(eventId, OrderStatus.COMPLETED);
        BigDecimal grossRevenue = completedOrders.stream()
                .map(o -> o.getTotalAmount().add(o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscounts = completedOrders.stream()
                .map(o -> o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue = completedOrders.stream()
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var refundedOrders = orderRepository.findByEventIdAndStatus(eventId, OrderStatus.REFUNDED);
        BigDecimal totalRefunds = refundedOrders.stream()
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        analytics.put("grossRevenue", grossRevenue);
        analytics.put("totalDiscounts", totalDiscounts);
        analytics.put("totalRefunds", totalRefunds);
        analytics.put("netRevenue", netRevenue.subtract(totalRefunds));

        // Attendance
        long checkedIn = checkInRecordRepository.countByEventIdAndReversedFalse(eventId);
        long validTickets = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.VALID);
        long checkedInTickets = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.CHECKED_IN);

        analytics.put("checkedIn", checkedIn);
        analytics.put("totalAttendees", validTickets + checkedInTickets);
        analytics.put("checkInPercentage", (validTickets + checkedInTickets) > 0 ?
                (checkedIn * 100.0 / (validTickets + checkedInTickets)) : 0);

        // Orders
        analytics.put("totalOrders", orderRepository.countByEventIdAndStatus(eventId, OrderStatus.COMPLETED));

        // Sales by ticket type
        var ticketTypeSales = ticketTypes.stream().map(tt -> Map.of(
                "name", tt.getName(),
                "sold", tt.getSold(),
                "quantity", tt.getQuantity(),
                "revenue", tt.getPrice().multiply(BigDecimal.valueOf(tt.getSold()))
        )).toList();
        analytics.put("salesByTicketType", ticketTypeSales);

        // Campaign stats
        var campaigns = campaignRepository.findByEventId(eventId);
        var campaignStats = campaigns.stream().map(c -> Map.of(
                "name", c.getName(),
                "redemptions", c.getCurrentRedemptions(),
                "maxRedemptions", c.getMaxRedemptions() != null ? c.getMaxRedemptions() : "unlimited"
        )).toList();
        analytics.put("campaigns", campaignStats);

        return analytics;
    }
}