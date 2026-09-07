package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.ticket.TicketTypeRequest;
import org.example.lateticket.entity.Event;
import org.example.lateticket.entity.TicketType;
import org.example.lateticket.entity.User;
import org.example.lateticket.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventService eventService;
    private final AuditService auditService;

    public TicketType createTicketType(Long eventId, TicketTypeRequest request, User organizer) {
        Event event = eventService.getEventEntity(eventId);
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        TicketType ticketType = TicketType.builder()
                .event(event)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .sold(0)
                .free(request.isFree())
                .hidden(false)
                .salesStart(request.getSalesStart())
                .salesEnd(request.getSalesEnd())
                .maxPerOrder(request.getMaxPerOrder())
                .build();

        ticketType = ticketTypeRepository.save(ticketType);
        auditService.log(eventId, organizer.getId(), "CREATE", "TicketType", ticketType.getId(),
                "Ticket type created: " + ticketType.getName());
        return ticketType;
    }

    public TicketType updateTicketType(Long ticketTypeId, TicketTypeRequest request, User organizer) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new RuntimeException("Ticket type not found"));

        if (!ticketType.getEvent().getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        ticketType.setName(request.getName());
        ticketType.setDescription(request.getDescription());
        ticketType.setPrice(request.getPrice());
        ticketType.setQuantity(request.getQuantity());
        ticketType.setFree(request.isFree());
        ticketType.setSalesStart(request.getSalesStart());
        ticketType.setSalesEnd(request.getSalesEnd());
        ticketType.setMaxPerOrder(request.getMaxPerOrder());

        return ticketTypeRepository.save(ticketType);
    }

    public void hideTicketType(Long ticketTypeId, User organizer) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new RuntimeException("Ticket type not found"));
        if (!ticketType.getEvent().getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }
        ticketType.setHidden(true);
        ticketTypeRepository.save(ticketType);
    }

    public List<TicketType> getTicketTypes(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId);
    }

    public List<TicketType> getVisibleTicketTypes(Long eventId) {
        return ticketTypeRepository.findByEventIdAndHiddenFalse(eventId);
    }
}