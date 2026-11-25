package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Notification;
import org.example.lateticket.entity.Order;
import org.example.lateticket.entity.User;
import org.example.lateticket.entity.enums.OrderStatus;
import org.example.lateticket.entity.enums.Role;
import org.example.lateticket.repository.NotificationRepository;
import org.example.lateticket.repository.OrderRepository;
import org.example.lateticket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-app notifications (SRS 4.10). */
@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_REFUNDED = "ORDER_REFUNDED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String EVENT_CANCELLED = "EVENT_CANCELLED";
    public static final String EVENT_UPDATED = "EVENT_UPDATED";
    public static final String SALES_SUSPENDED = "SALES_SUSPENDED";
    public static final String SUPPORT_REPLY = "SUPPORT_REPLY";
    public static final String SUPPORT_STATUS = "SUPPORT_STATUS";

    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public Notification notify(User user, String type, String title, String message, String link) {
        return notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .build());
    }

    /** Notifies every attendee holding a pending or completed order for the event (once per attendee). */
    public void notifyEventAttendees(Long eventId, String type, String title, String message) {
        Map<Long, User> attendees = new LinkedHashMap<>();
        for (Order order : orderRepository.findByEventId(eventId)) {
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.PENDING) {
                attendees.putIfAbsent(order.getAttendee().getId(), order.getAttendee());
            }
        }
        attendees.values().forEach(u -> notify(u, type, title, message, "/events/" + eventId));
    }

    public void notifyPlatformAdmins(String type, String title, String message, String link) {
        userRepository.findByRole(Role.PLATFORM_ADMIN).forEach(u -> notify(u, type, title, message, link));
    }

    public List<Notification> getAll(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<Notification> getUnread(User user) {
        return notificationRepository.findByUserIdAndReadFalse(user.getId());
    }

    public long countUnread(User user) {
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    public void markAsRead(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Notification not found");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(user.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
