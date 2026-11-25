package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Event;
import org.example.lateticket.entity.OrganizerProfile;
import org.example.lateticket.entity.User;
import org.example.lateticket.repository.AuditLogRepository;
import org.example.lateticket.repository.EventRepository;
import org.example.lateticket.repository.OrganizerProfileRepository;
import org.example.lateticket.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivationService {

    private final EventRepository eventRepository;
    private final OrganizerProfileRepository organizerProfileRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    public Map<String, Object> getActivationChecklist(Long eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        OrganizerProfile profile = organizerProfileRepository.findByUserId(organizer.getId())
                .orElse(null);

        boolean hasPaidTicketType = ticketTypeRepository.findByEventId(eventId).stream()
                .anyMatch(tt -> !tt.isFree());
        boolean hasIdentityVerification = profile != null && profile.isIdentityVerified();
        boolean hasPayoutAccount = profile != null && profile.isPayoutVerified();

        Map<String, Object> checklist = new LinkedHashMap<>();
        checklist.put("hasPaidTicketType", hasPaidTicketType);
        checklist.put("identityVerified", hasIdentityVerification);
        checklist.put("payoutVerified", hasPayoutAccount);
        checklist.put("activationFeePaid", event.isPaidSalesActivated()); // Simplified for MVP
        checklist.put("allRequirementsMet", hasPaidTicketType && hasIdentityVerification && hasPayoutAccount);
        checklist.put("alreadyActivated", event.isPaidSalesActivated());

        return checklist;
    }

    public Map<String, Object> activatePaidSales(Long eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        if (event.isPaidSalesActivated()) {
            throw new RuntimeException("Paid sales already activated");
        }
        // An admin suspension can only be lifted by an admin (POST /api/admin/events/{id}/resume-sales)
        boolean suspendedByAdmin = auditLogRepository.findByEventIdOrderByTimestampDesc(eventId).stream()
                .filter(l -> "SUSPEND_PAID_SALES".equals(l.getActionType()) || "RESUME_PAID_SALES".equals(l.getActionType()))
                .findFirst()
                .map(l -> "SUSPEND_PAID_SALES".equals(l.getActionType()))
                .orElse(false);
        if (suspendedByAdmin) {
            throw new RuntimeException("Paid sales were suspended by a platform administrator. Contact support.");
        }

        // For academic MVP: simulate activation fee payment
        // In production, this would integrate with a real payment processor
        event.setPaidSalesActivated(true);
        eventRepository.save(event);

        auditService.log(eventId, organizer.getId(), "ACTIVATE_PAID_SALES", "Event", eventId,
                "Paid sales activated (simulated)");

        return Map.of("message", "Paid sales activated successfully (simulated)",
                "eventId", eventId, "activated", true);
    }
}