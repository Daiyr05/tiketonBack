package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.admin.*;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.EventStatus;
import org.example.lateticket.entity.enums.OrderStatus;
import org.example.lateticket.entity.enums.ReportStatus;
import org.example.lateticket.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Platform admin features (SRS 4.5, 4.12). */
@Service
@RequiredArgsConstructor
public class AdminService {

    static final String ACTIVATE = "ACTIVATE_PAID_SALES";
    static final String SUSPEND = "SUSPEND_PAID_SALES";
    static final String RESUME = "RESUME_PAID_SALES";

    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RefundRepository refundRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventReportRepository eventReportRepository;
    private final PlatformSettingsRepository platformSettingsRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ---- Events ----

    @Transactional
    public void suspendEvent(Long eventId, User admin) {
        Event event = getEvent(eventId);
        event.setStatus(EventStatus.CANCELLED);
        event.setPaidSalesActivated(false);
        eventRepository.save(event);
        auditService.log(eventId, admin.getId(), "ADMIN_SUSPEND_EVENT", "Event", eventId, "Event suspended by platform admin");
        notificationService.notify(event.getOrganizer(), NotificationService.EVENT_CANCELLED,
                "Event suspended: " + event.getTitle(),
                "A platform administrator suspended \"" + event.getTitle() + "\". Contact support for details.",
                "/events/" + eventId);
        notificationService.notifyEventAttendees(eventId, NotificationService.EVENT_CANCELLED,
                "Event cancelled: " + event.getTitle(),
                "\"" + event.getTitle() + "\" has been cancelled.");
    }

    /** Stops paid ticket sales only; the event and free tickets stay available (SRS 4.5). */
    @Transactional
    public void suspendPaidSales(Long eventId, String reason, User admin) {
        Event event = getEvent(eventId);
        if (!event.isPaidSalesActivated()) {
            throw new RuntimeException("Paid sales are not active for this event");
        }
        event.setPaidSalesActivated(false);
        eventRepository.save(event);
        auditService.log(eventId, admin.getId(), SUSPEND, "Event", eventId,
                "Paid sales suspended by platform admin" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        notificationService.notify(event.getOrganizer(), NotificationService.SALES_SUSPENDED,
                "Paid sales suspended: " + event.getTitle(),
                "A platform administrator suspended paid ticket sales for \"" + event.getTitle() + "\"."
                        + (reason != null && !reason.isBlank() ? " Reason: " + reason : ""),
                "/events/" + eventId);
    }

    @Transactional
    public void resumePaidSales(Long eventId, User admin) {
        Event event = getEvent(eventId);
        if (event.isPaidSalesActivated()) {
            throw new RuntimeException("Paid sales are already active for this event");
        }
        event.setPaidSalesActivated(true);
        eventRepository.save(event);
        auditService.log(eventId, admin.getId(), RESUME, "Event", eventId, "Paid sales resumed by platform admin");
        notificationService.notify(event.getOrganizer(), NotificationService.SALES_SUSPENDED,
                "Paid sales resumed: " + event.getTitle(),
                "Paid ticket sales for \"" + event.getTitle() + "\" are active again.",
                "/events/" + eventId);
    }

    public List<ActivationRecordResponse> getActivationRecords() {
        List<AuditLog> logs = auditLogRepository.findByActionTypeInOrderByTimestampDesc(List.of(ACTIVATE, SUSPEND, RESUME));
        Map<Long, Event> events = eventRepository.findAllById(
                        logs.stream().map(AuditLog::getEventId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Event::getId, Function.identity()));
        return logs.stream().map(log -> {
            Event event = events.get(log.getEventId());
            return ActivationRecordResponse.builder()
                    .id(log.getId())
                    .eventId(log.getEventId())
                    .eventTitle(event != null ? event.getTitle() : null)
                    .organizerId(event != null ? event.getOrganizer().getId() : null)
                    .organizerEmail(event != null ? event.getOrganizer().getEmail() : null)
                    .action(log.getActionType())
                    .performedBy(log.getUserId())
                    .description(log.getDescription())
                    .paidSalesActivated(event != null && event.isPaidSalesActivated())
                    .timestamp(log.getTimestamp())
                    .build();
        }).toList();
    }

    // ---- Event reports ----

    @Transactional
    public EventReportResponse reportEvent(Long eventId, String reason, User reporter) {
        Event event = getEvent(eventId);
        if (eventReportRepository.existsByEventIdAndReporterIdAndStatus(eventId, reporter.getId(), ReportStatus.OPEN)) {
            throw new RuntimeException("You already have an open report for this event");
        }
        EventReport report = eventReportRepository.save(EventReport.builder()
                .event(event)
                .reporter(reporter)
                .reason(reason.trim())
                .status(ReportStatus.OPEN)
                .build());
        notificationService.notifyPlatformAdmins("EVENT_REPORTED", "Event reported: " + event.getTitle(),
                reason.length() > 200 ? reason.substring(0, 200) + "..." : reason, "/admin/reports");
        return toResponse(report);
    }

    public List<EventReportResponse> getReports(String status) {
        List<EventReport> reports = status == null || status.isBlank()
                ? eventReportRepository.findAllByOrderByCreatedAtDesc()
                : eventReportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.valueOf(status.trim().toUpperCase()));
        return reports.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventReportResponse updateReport(Long reportId, ReportUpdateRequest request, User admin) {
        EventReport report = eventReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        ReportStatus status = ReportStatus.valueOf(request.getStatus().trim().toUpperCase());
        report.setStatus(status);
        report.setAdminNote(request.getAdminNote());
        report.setResolvedAt(status == ReportStatus.OPEN ? null : LocalDateTime.now());
        auditService.log(report.getEvent().getId(), admin.getId(), "REPORT_" + status, "EventReport", reportId,
                "Event report " + status.name().toLowerCase());
        return toResponse(eventReportRepository.save(report));
    }

    private EventReportResponse toResponse(EventReport r) {
        return EventReportResponse.builder()
                .id(r.getId())
                .eventId(r.getEvent().getId())
                .eventTitle(r.getEvent().getTitle())
                .eventStatus(r.getEvent().getStatus().name())
                .reporterId(r.getReporter().getId())
                .reporterEmail(r.getReporter().getEmail())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .adminNote(r.getAdminNote())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }

    // ---- Platform report and settings ----

    /** Defaults to the last 30 days. */
    public PlatformReportResponse getPlatformReport(LocalDateTime from, LocalDateTime to) {
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusDays(30);
        if (!start.isBefore(end)) {
            throw new RuntimeException("'from' must be before 'to'");
        }

        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        // Orders that were paid at some point: completed now, or completed and refunded later
        List<Order> paid = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.REFUNDED)
                .toList();
        List<Refund> refunds = refundRepository.findByCreatedAtBetween(start, end);

        BigDecimal gross = sum(paid.stream().map(Order::getTotalAmount));
        BigDecimal refundsAmount = sum(refunds.stream().map(Refund::getAmount));
        long ticketsSold = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .flatMap(o -> orderItemRepository.findByOrderId(o.getId()).stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();

        return PlatformReportResponse.builder()
                .from(start)
                .to(end)
                .ordersTotal(orders.size())
                .ordersCompleted(count(orders, OrderStatus.COMPLETED))
                .ordersPending(count(orders, OrderStatus.PENDING))
                .ordersCancelled(count(orders, OrderStatus.CANCELLED))
                .ordersRefunded(count(orders, OrderStatus.REFUNDED))
                .ticketsSold(ticketsSold)
                .grossRevenue(gross)
                .discounts(sum(paid.stream().map(Order::getDiscountAmount)))
                .serviceFees(sum(orders.stream()
                        .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                        .map(Order::getServiceFee)))
                .refundsCount(refunds.size())
                .refundsAmount(refundsAmount)
                .netRevenue(gross.subtract(refundsAmount))
                .build();
    }

    public PlatformSettingsDto getSettings() {
        PlatformSettings s = loadSettings();
        return new PlatformSettingsDto(s.getServiceFeePercent(), s.getServiceFeeFixed(), s.getUpdatedAt());
    }

    @Transactional
    public PlatformSettingsDto updateSettings(PlatformSettingsDto request, User admin) {
        PlatformSettings s = loadSettings();
        s.setServiceFeePercent(request.getServiceFeePercent());
        s.setServiceFeeFixed(request.getServiceFeeFixed());
        s = platformSettingsRepository.saveAndFlush(s);
        auditService.log(null, admin.getId(), "UPDATE_SERVICE_FEE", "PlatformSettings", s.getId(),
                "Service fee set to " + s.getServiceFeePercent() + "% + " + s.getServiceFeeFixed() + " KZT");
        return new PlatformSettingsDto(s.getServiceFeePercent(), s.getServiceFeeFixed(), s.getUpdatedAt());
    }

    private PlatformSettings loadSettings() {
        return platformSettingsRepository.findById(PlatformSettings.SINGLETON_ID)
                .orElseGet(() -> PlatformSettings.builder()
                        .id(PlatformSettings.SINGLETON_ID)
                        .serviceFeePercent(BigDecimal.ZERO)
                        .serviceFeeFixed(BigDecimal.ZERO)
                        .build());
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));
    }

    private static long count(List<Order> orders, OrderStatus status) {
        return orders.stream().filter(o -> o.getStatus() == status).count();
    }

    private static BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
        return values.filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
