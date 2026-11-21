package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Event;
import org.example.lateticket.entity.Order;
import org.example.lateticket.entity.Ticket;
import org.example.lateticket.entity.User;
import org.example.lateticket.entity.enums.Role;
import org.example.lateticket.repository.StaffAssignmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Ownership checks for orders and tickets.
 * Access is granted to the attendee who owns the order/ticket, the event's organizer,
 * staff assigned to the event, and platform admins.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final StaffAssignmentRepository staffAssignmentRepository;

    public boolean canManageEvent(Event event, User user) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.PLATFORM_ADMIN) {
            return true;
        }
        if (event.getOrganizer() != null && event.getOrganizer().getId().equals(user.getId())) {
            return true;
        }
        return staffAssignmentRepository.existsByEventIdAndUserId(event.getId(), user.getId());
    }

    public void checkCanManageEvent(Event event, User user) {
        if (!canManageEvent(event, user)) {
            throw new AccessDeniedException("Not authorized");
        }
    }

    public void checkCanViewOrder(Order order, User user) {
        boolean owner = user != null && order.getAttendee().getId().equals(user.getId());
        if (!owner && !canManageEvent(order.getEvent(), user)) {
            throw new AccessDeniedException("Not authorized");
        }
    }

    public void checkCanViewTicket(Ticket ticket, User user) {
        boolean owner = user != null && (ticket.getAttendee().getId().equals(user.getId())
                || ticket.getOrder().getAttendee().getId().equals(user.getId()));
        if (!owner && !canManageEvent(ticket.getEvent(), user)) {
            throw new AccessDeniedException("Not authorized");
        }
    }
}
