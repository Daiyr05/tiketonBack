package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.order.*;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.*;
import org.example.lateticket.repository.*;
import org.example.lateticket.entity.Refund;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            TicketType ticketType = ticketTypeRepository.findById(itemReq.getTicketTypeId())
                    .orElseThrow(() -> new RuntimeException("Ticket type not found"));

            if (!ticketType.getEvent().getId().equals(event.getId())) {
                throw new RuntimeException("Ticket type does not belong to this event");
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

            BigDecimal itemTotal = ticketType.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = OrderItem.builder()
                    .ticketType(ticketType)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(ticketType.getPrice())
                    .discount(BigDecimal.ZERO)
                    .seatSection(itemReq.getSeatSection())
                    .seatRow(itemReq.getSeatRow())
                    .seatNumber(itemReq.getSeatNumber())
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getAttendee().getId().equals(attendee.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not pending payment");
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

    public OrderResponse cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean isAttendee = order.getAttendee().getId().equals(user.getId());
        boolean isOrganizer = order.getEvent().getOrganizer().getId().equals(user.getId());
        if (!isAttendee && !isOrganizer) {
            throw new RuntimeException("Not authorized");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Order cannot be cancelled");
        }

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

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toResponse(order);
    }

    public List<OrderResponse> getAttendeeOrders(Long attendeeId) {
        return orderRepository.findByAttendeeId(attendeeId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getEventOrders(Long eventId) {
        return orderRepository.findByEventId(eventId)
                .stream().map(this::toResponse).toList();
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(order);
    }

    private void generateTickets(Order order, List<OrderItem> items) {
        for (OrderItem item : items) {
            for (int i = 0; i < item.getQuantity(); i++) {
                Ticket ticket = Ticket.builder()
                        .ticketCode(UUID.randomUUID().toString())
                        .order(order)
                        .ticketType(item.getTicketType())
                        .event(order.getEvent())
                        .attendee(order.getAttendee())
                        .attendeeName(order.getAttendeeFirstName() + " " + order.getAttendeeLastName())
                        .attendeeEmail(order.getAttendeeEmail())
                        .status(TicketStatus.VALID)
                        .seatSection(item.getSeatSection())
                        .seatRow(item.getSeatRow())
                        .seatNumber(item.getSeatNumber())
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
                        .build()).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }
}