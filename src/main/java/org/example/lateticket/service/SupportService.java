package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.*;
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

        // Verify access
        boolean isOwner = supportCase.getUser().getId().equals(sender.getId());
        boolean isAssigned = supportCase.getAssignedTo() != null &&
                supportCase.getAssignedTo().getId().equals(sender.getId());
        boolean isEventOrganizer = supportCase.getEvent() != null &&
                supportCase.getEvent().getOrganizer().getId().equals(sender.getId());

        if (!isOwner && !isAssigned && !isEventOrganizer) {
            throw new RuntimeException("Not authorized to message in this case");
        }

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

    public List<SupportCase> getEventCases(Long eventId) {
        return supportCaseRepository.findByEventId(eventId);
    }

    public List<SupportMessage> getCaseMessages(Long caseId) {
        return supportMessageRepository.findBySupportCaseIdOrderByCreatedAtAsc(caseId);
    }
}