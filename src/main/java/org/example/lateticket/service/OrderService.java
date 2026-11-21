package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.order.*;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.*;
import org.example.lateticket.repository.*;
import org.example.lateticket.entity.Refund;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final AuditService auditService;
    private final SeatService seatService;
    private final AccessService accessService;

    // Unpaid PENDING orders hold inventory (tickets, seats, promo redemption) only for this long
    @Value("${app.orders.pending-timeout-minutes:15}")
    private int pendingTimeoutMinutes;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, User attendee) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new RuntimeException("Event is not available for registration");
        }

        // Validate and calculate
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        boolean hasPaidTickets = false;
        boolean useSeatMap = event.isAssignedSeating() && seatService.hasSeatMap(event.getId());
        List<Seat> claimedSeats = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            TicketType ticketType = ticketTypeRepository.findById(itemReq.getTicketTypeId())
                    .orElseThrow(() -> new RuntimeException("Ticket type not found"));

            if (!ticketType.getEvent().getId().equals(event.getId())) {
                throw new RuntimeException("Ticket type does not belong to this event");
            }
            TicketSalesRules.assertOnSale(ticketType);

            if (itemReq.getQuantity() < 1) {
                throw new RuntimeException("Quantity must be at least 1 for: " + ticketType.getName());
            }

            int available = ticketType.getQuantity() - ticketType.getSold();
            if (itemReq.getQuantity() > available) {
                throw new RuntimeException("Not enough tickets available for: " + ticketType.getName());
            }

            if (ticketType.getMaxPerOrder() != null && itemReq.getQuantity() > ticketType.getMaxPerOrder()) {
                throw new RuntimeException("Exceeds max per order for: " + ticketType.getName());
            }

            if (!ticketType.isFree()) {
                hasPaidTickets = true;
                if (!event.isPaidSalesActivated()) {
                    throw new RuntimeException("Paid sales not activated for this event");
                }
            }

            List<Seat> itemSeats = List.of();
            if (useSeatMap) {
                itemSeats = seatService.claimHeldSeats(event, ticketType, itemReq.getSeatIds(),
                        itemReq.getQuantity(), attendee);
                claimedSeats.addAll(itemSeats);
            }
            // A single seat is also mirrored onto the item; multi-seat items keep them on the seats table
            Seat singleSeat = itemSeats.size() == 1 ? itemSeats.get(0) : null;

            BigDecimal itemTotal = ticketType.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = OrderItem.builder()
                    .ticketType(ticketType)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(ticketType.getPrice())
                    .discount(BigDecimal.ZERO)
                    .seatSection(useSeatMap ? (singleSeat != null ? singleSeat.getSection() : null) : itemReq.getSeatSection())
                    .seatRow(useSeatMap ? (singleSeat != null ? singleSeat.getRowLabel() : null) : itemReq.getSeatRow())
                    .seatNumber(useSeatMap ? (singleSeat != null ? singleSeat.getSeatNumber() : null) : itemReq.getSeatNumber())
                    .build();
            items.add(item);
        }

        // Apply promo code
        PromoCode promoCode = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            promoCode = promoCodeRepository.findByCode(request.getPromoCode())
                    .orElseThrow(() -> new RuntimeException("Invalid promo code"));

            PromotionalCampaign campaign = promoCode.getCampaign();
            if (!campaign.isActive()) {
                throw new RuntimeException("Promo code is no longer active");
            }
            if (campaign.getValidFrom() != null && LocalDateTime.now().isBefore(campaign.getValidFrom())) {
                throw new RuntimeException("Promo code is not yet valid");
            }
            if (campaign.getValidTo() != null && LocalDateTime.now().isAfter(campaign.getValidTo())) {
                throw new RuntimeException("Promo code has expired");
            }
            if (campaign.getMaxRedemptions() != null &&
                    campaign.getCurrentRedemptions() >= campaign.getMaxRedemptions()) {
                throw new RuntimeException("Promo code has reached its redemption limit");
            }

            if (campaign.getDiscountType() == DiscountType.PERCENTAGE) {
                totalDiscount = totalAmount.multiply(campaign.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            } else {
                totalDiscount = campaign.getDiscountValue().min(totalAmount);
            }

            totalAmount = totalAmount.subtract(totalDiscount);
            campaign.setCurrentRedemptions(campaign.getCurrentRedemptions() + 1);
        }

        // Create order
        Order order = Order.builder()
                .attendee(attendee)
                .event(event)
                .totalAmount(totalAmount)
                .discountAmount(totalDiscount)
                .status(OrderStatus.PENDING)
                .promoCode(promoCode)
                .attendeeFirstName(request.getAttendeeFirstName() != null ?
                        request.getAttendeeFirstName() : attendee.getFirstName())
                .attendeeLastName(request.getAttendeeLastName() != null ?
                        request.getAttendeeLastName() : attendee.getLastName())
                .attendeeEmail(request.getAttendeeEmail() != null ?
                        request.getAttendeeEmail() : attendee.getEmail())
                .build();

        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrder(order);
        }
        orderItemRepository.saveAll(items);
        order.setItems(items);
        seatService.markSold(claimedSeats, order);

        // Update inventory
        for (OrderItem item : items) {
            TicketType tt = item.getTicketType();
            tt.setSold(tt.getSold() + item.getQuantity());
            ticketTypeRepository.save(tt);
        }

        // For free orders, auto-complete and generate tickets
        boolean isFreeOrder = totalAmount.compareTo(BigDecimal.ZERO) == 0;
        if (isFreeOrder) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            generateTickets(order, items);

            // Create simulated payment record
            Payment payment = Payment.builder()
                    .order(order)
                    .amount(BigDecimal.ZERO)
                    .method("FREE")
                    .status(PaymentStatus.COMPLETED)
                    .transactionRef("FREE-" + UUID.randomUUID().toString().substring(0, 8))
                    .simulated(true)
                    .build();
            paymentRepository.save(payment);
        }

        auditService.log(event.getId(), attendee.getId(), "CREATE", "Order", order.getId(),
                "Order created, total: " + order.getTotalAmount() + " KZT");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse completePayment(Long orderId, User attendee) {
        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getAttendee().getId().equals(attendee.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not pending payment");
        }
        if (LocalDateTime.now().isAfter(paymentDeadline(order))) {
            // The expiry job will cancel it and release the inventory
            throw new RuntimeException("Payment window has expired, please place a new order");
        }

        // Simulated payment
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method("SIMULATED_CARD")
                .status(PaymentStatus.COMPLETED)
                .transactionRef("SIM-" + UUID.randomUUID().toString().substring(0, 8))
                .simulated(true)
                .build();
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        generateTickets(order, items);

        auditService.log(order.getEvent().getId(), attendee.getId(), "PAYMENT", "Order", order.getId(),
                "Payment completed (simulated): " + order.getTotalAmount() + " KZT");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse refundOrder(Long orderId, User organizer, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getEvent().getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Only completed orders can be refunded");
        }

        // Invalidate tickets
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.REFUNDED);
            ticketRepository.save(ticket);
        }

        // Restore inventory
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            TicketType tt = item.getTicketType();
            tt.setSold(tt.getSold() - item.getQuantity());
            ticketTypeRepository.save(tt);
        }
        seatService.releaseOrderSeats(orderId);

        // Create refund record
        Refund refund = Refund.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .reason(reason)
                .initiatedBy(organizer)
                .build();
        refundRepository.save(refund);

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        auditService.log(order.getEvent().getId(), organizer.getId(), "REFUND", "Order", order.getId(),
                "Order refunded: " + order.getTotalAmount() + " KZT");

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, User user) {
        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean isAttendee = order.getAttendee().getId().equals(user.getId());
        boolean isOrganizer = order.getEvent().getOrganizer().getId().equals(user.getId());
        if (!isAttendee && !isOrganizer) {
            throw new RuntimeException("Not authorized");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        cancelAndRelease(order);
        return toResponse(order);
    }

    /**
     * Cancels a PENDING order whose payment window has passed, releasing its tickets, seats and promo redemption.
     * Called by {@link OrderExpiryJob}; re-checks the state under a row lock so it can't race with payment.
     */
    @Transactional
    public boolean expireIfStale(Long orderId) {
        Order order = orderRepository.lockById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING
                || !LocalDateTime.now().isAfter(paymentDeadline(order))) {
            return false;
        }
        cancelAndRelease(order);
        auditService.log(order.getEvent().getId(), order.getAttendee().getId(), "EXPIRE", "Order", order.getId(),
                "Unpaid order cancelled after " + pendingTimeoutMinutes + " minutes");
        return true;
    }

    public List<Long> findStalePendingOrderIds() {
        return orderRepository.findIdsByStatusAndCreatedAtBefore(OrderStatus.PENDING,
                LocalDateTime.now().minusMinutes(pendingTimeoutMinutes));
    }

    private void cancelAndRelease(Order order) {
        Long orderId = order.getId();

        // Invalidate tickets
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        }

        // Restore inventory
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            TicketType tt = item.getTicketType();
            tt.setSold(tt.getSold() - item.getQuantity());
            ticketTypeRepository.save(tt);
        }
        seatService.releaseOrderSeats(orderId);

        // Give the promo redemption back
        if (order.getPromoCode() != null) {
            PromotionalCampaign campaign = order.getPromoCode().getCampaign();
            campaign.setCurrentRedemptions(Math.max(0, campaign.getCurrentRedemptions() - 1));
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private LocalDateTime paymentDeadline(Order order) {
        return order.getCreatedAt().plusMinutes(pendingTimeoutMinutes);
    }

    public List<OrderResponse> getAttendeeOrders(Long attendeeId) {
        return orderRepository.findByAttendeeId(attendeeId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getEventOrders(Long eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        accessService.checkCanManageEvent(event, user);
        return orderRepository.findByEventId(eventId)
                .stream().map(this::toResponse).toList();
    }

    public OrderResponse getOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        accessService.checkCanViewOrder(order, user);
        return toResponse(order);
    }

    private void generateTickets(Order order, List<OrderItem> items) {
        Map<Long, List<Seat>> seatsByTicketType = new HashMap<>();
        for (Seat seat : seatService.getOrderSeats(order.getId())) {
            seatsByTicketType.computeIfAbsent(seat.getTicketType().getId(), k -> new ArrayList<>()).add(seat);
        }

        for (OrderItem item : items) {
            List<Seat> seats = seatsByTicketType.getOrDefault(item.getTicketType().getId(), new ArrayList<>());
            for (int i = 0; i < item.getQuantity(); i++) {
                Seat seat = seats.isEmpty() ? null : seats.remove(0);
                Ticket ticket = Ticket.builder()
                        .ticketCode(UUID.randomUUID().toString())
                        .order(order)
                        .ticketType(item.getTicketType())
                        .event(order.getEvent())
                        .attendee(order.getAttendee())
                        .attendeeName(order.getAttendeeFirstName() + " " + order.getAttendeeLastName())
                        .attendeeEmail(order.getAttendeeEmail())
                        .status(TicketStatus.VALID)
                        .seatSection(seat != null ? seat.getSection() : item.getSeatSection())
                        .seatRow(seat != null ? seat.getRowLabel() : item.getSeatRow())
                        .seatNumber(seat != null ? seat.getSeatNumber() : item.getSeatNumber())
                        .build();
                ticketRepository.save(ticket);
            }
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            items = orderItemRepository.findByOrderId(order.getId());
        }
        Map<Long, List<String>> seatLabels = new HashMap<>();
        for (Seat seat : seatService.getOrderSeats(order.getId())) {
            seatLabels.computeIfAbsent(seat.getTicketType().getId(), k -> new ArrayList<>())
                    .add(seat.getSection() + "-" + seat.getRowLabel() + "-" + seat.getSeatNumber());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .eventId(order.getEvent().getId())
                .eventTitle(order.getEvent().getTitle())
                .attendeeId(order.getAttendee().getId())
                .attendeeName(order.getAttendeeFirstName() + " " + order.getAttendeeLastName())
                .attendeeEmail(order.getAttendeeEmail())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .status(order.getStatus().name())
                .promoCode(order.getPromoCode() != null ? order.getPromoCode().getCode() : null)
                .items(items.stream().map(item -> OrderResponse.ItemResponse.builder()
                        .id(item.getId())
                        .ticketTypeName(item.getTicketType().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discount(item.getDiscount())
                        .seats(seatLabels.getOrDefault(item.getTicketType().getId(), List.of()))
                        .build()).toList())
                .createdAt(order.getCreatedAt())
                .paymentDeadline(order.getStatus() == OrderStatus.PENDING ? paymentDeadline(order) : null)
                .build();
    }
}