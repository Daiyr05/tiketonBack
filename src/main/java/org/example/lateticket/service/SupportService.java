package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.Role;
import org.example.lateticket.entity.enums.SupportCaseStatus;
import org.example.lateticket.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportCaseRepository supportCaseRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;

    public SupportCase createCase(User user, String category, String subject, String message,
                                   Long eventId, Long orderId, Long ticketId) {
        SupportCase supportCase = SupportCase.builder()
                .user(user)
                .category(category)
                .subject(subject)
                .status(SupportCaseStatus.OPEN)
                .build();

        if (eventId != null) {
            supportCase.setEvent(eventRepository.findById(eventId).orElse(null));
        }
        if (orderId != null) {
            supportCase.setOrder(orderRepository.findById(orderId).orElse(null));
        }
        if (ticketId != null) {
            supportCase.setTicket(ticketRepository.findById(ticketId).orElse(null));
        }

        supportCase = supportCaseRepository.save(supportCase);

        // Add initial message
        SupportMessage msg = SupportMessage.builder()
                .supportCase(supportCase)
                .sender(user)
                .message(message)
                .build();
        supportMessageRepository.save(msg);

        return supportCase;
    }

    public SupportMessage addMessage(Long caseId, User sender, String message) {
        SupportCase supportCase = supportCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Support case not found"));

        requireAccess(supportCase, sender);

        SupportMessage msg = SupportMessage.builder()
                .supportCase(supportCase)
                .sender(sender)
                .message(message)
                .build();
        return supportMessageRepository.save(msg);
    }

    public void updateCaseStatus(Long caseId, String status, User user) {
        SupportCase supportCase = supportCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Support case not found"));
        requireAccess(supportCase, user);
        supportCase.setStatus(SupportCaseStatus.valueOf(status.toUpperCase()));
        supportCaseRepository.save(supportCase);
    }

    public void assignCase(Long caseId, User assignedTo) {
        SupportCase supportCase = supportCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Support case not found"));
        supportCase.setAssignedTo(assignedTo);
        supportCase.setStatus(SupportCaseStatus.IN_PROGRESS);
        supportCaseRepository.save(supportCase);
    }

    public List<SupportCase> getUserCases(Long userId) {
        return supportCaseRepository.findByUserId(userId);
    }

    public List<SupportCase> getEventCases(Long eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!isPlatformAdmin(user) && !event.getOrganizer().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to view cases for this event");
        }
        return supportCaseRepository.findByEventId(eventId);
    }

    public List<SupportMessage> getCaseMessages(Long caseId, User user) {
        SupportCase supportCase = supportCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Support case not found"));
        requireAccess(supportCase, user);
        return supportMessageRepository.findBySupportCaseIdOrderByCreatedAtAsc(caseId);
    }

    /** Platform cases (no event attached), newest first. For PLATFORM_ADMIN only. */
    public List<SupportCase> getPlatformCases(String status) {
        if (status == null || status.isBlank()) {
            return supportCaseRepository.findByEventIsNullOrderByCreatedAtDesc();
        }
        return supportCaseRepository.findByEventIsNullAndStatusOrderByCreatedAtDesc(
                SupportCaseStatus.valueOf(status.trim().toUpperCase()));
    }

    /** SRS 4.13: only the author, the assignee, the event organizer or a platform admin may access a case. */
    private void requireAccess(SupportCase supportCase, User user) {
        boolean isOwner = supportCase.getUser().getId().equals(user.getId());
        boolean isAssigned = supportCase.getAssignedTo() != null &&
                supportCase.getAssignedTo().getId().equals(user.getId());
        boolean isEventOrganizer = supportCase.getEvent() != null &&
                supportCase.getEvent().getOrganizer().getId().equals(user.getId());

        if (!isOwner && !isAssigned && !isEventOrganizer && !isPlatformAdmin(user)) {
            throw new RuntimeException("Not authorized to access this support case");
        }
    }

    private boolean isPlatformAdmin(User user) {
        return user.getRole() == Role.PLATFORM_ADMIN;
    }
}