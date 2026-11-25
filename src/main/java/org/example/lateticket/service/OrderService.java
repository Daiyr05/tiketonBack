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
    private final PlatformSettingsRepository platformSettingsRepository;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final PdfTicketService pdfTicketService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

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
            if (!campaign.getEvent().getId().equals(event.getId())) {
                throw new RuntimeException("Promo code is not valid for this event");
            }
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

            // Discount only the ticket types the campaign applies to (SRS 4.14)
            BigDecimal eligibleAmount = BigDecimal.ZERO;
            for (OrderItem item : items) {
                if (campaign.appliesTo(item.getTicketType().getId())) {
                    eligibleAmount = eligibleAmount.add(
                            item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
            if (eligibleAmount.signum() == 0) {
                throw new RuntimeException("Promo code does not apply to the selected tickets");
            }

            if (campaign.getDiscountType() == DiscountType.PERCENTAGE) {
                totalDiscount = eligibleAmount.multiply(campaign.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            } else {
                totalDiscount = campaign.getDiscountValue().min(eligibleAmount);
            }

            totalAmount = totalAmount.subtract(totalDiscount);
            campaign.setCurrentRedemptions(campaign.getCurrentRedemptions() + 1);
        }

        // Platform service fee on paid orders (SRS 4.6), included in the total
        BigDecimal serviceFee = calculateServiceFee(totalAmount);
        totalAmount = totalAmount.add(serviceFee);

        // Create order
        Order order = Order.builder()
                .attendee(attendee)
                .event(event)
                .totalAmount(totalAmount)
                .discountAmount(totalDiscount)
                .serviceFee(serviceFee)
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
            List<Ticket> tickets = generateTickets(order, items);
            onOrderCompleted(order, tickets, true);

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
        List<Ticket> tickets = generateTickets(order, items);
        onOrderCompleted(order, tickets, false);

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
        notificationService.notify(order.getAttendee(), NotificationService.ORDER_REFUNDED,
                "Order #" + order.getId() + " refunded",
                order.getTotalAmount() + " KZT for \"" + order.getEvent().getTitle() + "\" will be returned."
                        + (reason != null && !reason.isBlank() ? " Reason: " + reason : ""),
                "/orders/" + order.getId());

        return toResponse(order);
    }

    @Transactional
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
        seatService.releaseOrderSeats(orderId);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (!isAttendee) {
            notificationService.notify(order.getAttendee(), NotificationService.ORDER_CANCELLED,
                    "Order #" + order.getId() + " cancelled",
                    "Your order for \"" + order.getEvent().getTitle() + "\" was cancelled by the organizer.",
                    "/orders/" + order.getId());
        }

        return toResponse(order);
    }

    public List<OrderResponse> getAttendeeOrders(Long attendeeId) {
        return orderRepository.findByAttendeeId(attendeeId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getEventOrders(Long eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (user.getRole() != Role.PLATFORM_ADMIN && !event.getOrganizer().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }
        return orderRepository.findByEventId(eventId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    public OrderResponse getOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        boolean allowed = order.getAttendee().getId().equals(user.getId())
                || order.getEvent().getOrganizer().getId().equals(user.getId())
                || user.getRole() == Role.PLATFORM_ADMIN;
        if (!allowed) {
            throw new RuntimeException("Order not found");
        }
        return toResponse(order);
    }

    private BigDecimal calculateServiceFee(BigDecimal amount) {
        if (amount.signum() <= 0) return BigDecimal.ZERO;
        PlatformSettings settings = platformSettingsRepository.findById(PlatformSettings.SINGLETON_ID).orElse(null);
        if (settings == null) return BigDecimal.ZERO;
        BigDecimal fee = amount.multiply(settings.getServiceFeePercent())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                .add(settings.getServiceFeeFixed());
        return fee.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Notification + email with PDF tickets once an order is completed (SRS 4.4, 4.7, 4.10). */
    private void onOrderCompleted(Order order, List<Ticket> tickets, boolean free) {
        Event event = order.getEvent();
        String link = "/orders/" + order.getId();
        notificationService.notify(order.getAttendee(), NotificationService.ORDER_CONFIRMED,
                free ? "Registration confirmed" : "Payment received",
                (free ? "You are registered for \"" : "Your tickets for \"") + event.getTitle() + "\" are ready.",
                link);

        List<MailService.Attachment> attachments = new ArrayList<>();
        if (mailService.isEnabled()) {
            for (Ticket ticket : tickets) {
                attachments.add(new MailService.Attachment("ticket-" + ticket.getTicketCode().substring(0, 8) + ".pdf",
                        "application/pdf", pdfTicketService.generateTicketPdf(ticket)));
            }
        }
        StringBuilder text = new StringBuilder()
                .append("Hello ").append(order.getAttendeeFirstName()).append(",\n\n")
                .append(free ? "Your registration is confirmed for:\n" : "Thank you for your purchase! Your tickets for:\n")
                .append(event.getTitle()).append("\n");
        if (event.getEventDate() != null) text.append("Date: ").append(event.getEventDate()).append(" (UTC)\n");
        if (event.getVenueName() != null) text.append("Venue: ").append(event.getVenueName()).append("\n");
        text.append("\nOrder #").append(order.getId()).append(", tickets: ").append(tickets.size());
        if (!free) text.append(", total: ").append(order.getTotalAmount()).append(" KZT");
        text.append("\n\nYour tickets are attached as PDF and available at:\n")
                .append(frontendUrl).append(link).append("\n");

        mailService.sendAfterCommit(order.getAttendeeEmail(),
                (free ? "BiletFlow: registration confirmed - " : "BiletFlow: your tickets - ") + event.getTitle(),
                text.toString(), attachments);
    }

    private List<Ticket> generateTickets(Order order, List<OrderItem> items) {
        List<Ticket> created = new ArrayList<>();
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
                created.add(ticketRepository.save(ticket));
            }
        }
        return created;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            items = orderItemRepository.findByOrderId(order.getId());
        }
        BigDecimal fee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;
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
                .serviceFee(fee)
                .subtotal(order.getTotalAmount().subtract(fee)
                        .add(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO))
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
                .build();
    }
}