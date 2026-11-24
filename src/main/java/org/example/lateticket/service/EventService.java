package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.event.*;
import org.example.lateticket.entity.Event;
import org.example.lateticket.entity.User;
import org.example.lateticket.entity.enums.EventStatus;
import org.example.lateticket.entity.enums.EventVisibility;
import org.example.lateticket.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final AuditService auditService;

    public EventResponse createEvent(EventRequest request, User organizer) {
        Event event = Event.builder()
                .organizer(organizer)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .venueName(request.getVenueName())
                .venueAddress(request.getVenueAddress())
                .city(request.getCity())
                .eventDate(request.getEventDate())
                .eventEndDate(request.getEventEndDate())
                .registrationOpenDate(request.getRegistrationOpenDate())
                .registrationCloseDate(request.getRegistrationCloseDate())
                .capacity(request.getCapacity())
                .status(EventStatus.DRAFT)
                .visibility(parseVisibility(request.getVisibility()))
                .paidSalesActivated(false)
                .assignedSeating(request.isAssignedSeating())
                .build();

        event = eventRepository.save(event);
        auditService.log(event.getId(), organizer.getId(), "CREATE", "Event", event.getId(), "Event created");
        return toResponse(event);
    }

    public EventResponse updateEvent(Long eventId, EventRequest request, User organizer) {
        Event event = getEventOwnedBy(eventId, organizer);

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setImageUrl(request.getImageUrl());
        event.setVenueName(request.getVenueName());
        event.setVenueAddress(request.getVenueAddress());
        event.setCity(request.getCity());
        event.setEventDate(request.getEventDate());
        event.setEventEndDate(request.getEventEndDate());
        event.setRegistrationOpenDate(request.getRegistrationOpenDate());
        event.setRegistrationCloseDate(request.getRegistrationCloseDate());
        event.setCapacity(request.getCapacity());
        event.setAssignedSeating(request.isAssignedSeating());
        if (request.getVisibility() != null) {
            event.setVisibility(parseVisibility(request.getVisibility()));
        }

        event = eventRepository.save(event);
        auditService.log(event.getId(), organizer.getId(), "UPDATE", "Event", event.getId(), "Event updated");
        return toResponse(event);
    }

    public EventResponse publishEvent(Long eventId, User organizer) {
        Event event = getEventOwnedBy(eventId, organizer);
        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);
        auditService.log(event.getId(), organizer.getId(), "PUBLISH", "Event", event.getId(), "Event published");
        return toResponse(event);
    }

    public EventResponse unpublishEvent(Long eventId, User organizer) {
        Event event = getEventOwnedBy(eventId, organizer);
        event.setStatus(EventStatus.DRAFT);
        event = eventRepository.save(event);
        auditService.log(event.getId(), organizer.getId(), "UNPUBLISH", "Event", event.getId(), "Event unpublished");
        return toResponse(event);
    }

    public EventResponse cancelEvent(Long eventId, User organizer) {
        Event event = getEventOwnedBy(eventId, organizer);
        event.setStatus(EventStatus.CANCELLED);
        event = eventRepository.save(event);
        auditService.log(event.getId(), organizer.getId(), "CANCEL", "Event", event.getId(), "Event cancelled");
        return toResponse(event);
    }

    public EventResponse duplicateEvent(Long eventId, User organizer) {
        Event original = getEventOwnedBy(eventId, organizer);
        Event copy = Event.builder()
                .organizer(organizer)
                .title(original.getTitle() + " (Copy)")
                .description(original.getDescription())
                .category(original.getCategory())
                .imageUrl(original.getImageUrl())
                .venueName(original.getVenueName())
                .venueAddress(original.getVenueAddress())
                .city(original.getCity())
                .capacity(original.getCapacity())
                .status(EventStatus.DRAFT)
                .visibility(original.getVisibility())
                .paidSalesActivated(false)
                .assignedSeating(original.isAssignedSeating())
                .build();
        copy = eventRepository.save(copy);
        return toResponse(copy);
    }

    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return toResponse(event);
    }

    public List<EventResponse> getPublicEvents() {
        return eventRepository.findByStatusAndVisibility(EventStatus.PUBLISHED, EventVisibility.PUBLIC)
                .stream().map(this::toResponse).toList();
    }

    public List<EventResponse> getOrganizerEvents(Long organizerId) {
        return eventRepository.findByOrganizerId(organizerId)
                .stream().map(this::toResponse).toList();
    }

    public List<EventResponse> getOrganizerEventsByStatus(Long organizerId, String status) {
        EventStatus eventStatus = EventStatus.valueOf(status.toUpperCase());
        return eventRepository.findByOrganizerIdAndStatus(organizerId, eventStatus)
                .stream().map(this::toResponse).toList();
    }

    public Event getEventEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }

    private Event getEventOwnedBy(Long eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized to modify this event");
        }
        return event;
    }

    private EventVisibility parseVisibility(String visibility) {
        if (visibility == null) return EventVisibility.PUBLIC;
        try {
            return EventVisibility.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EventVisibility.PUBLIC;
        }
    }

    public EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getFirstName() + " " + event.getOrganizer().getLastName())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .imageUrl(event.getImageUrl())
                .venueName(event.getVenueName())
                .venueAddress(event.getVenueAddress())
                .city(event.getCity())
                .eventDate(event.getEventDate())
                .eventEndDate(event.getEventEndDate())
                .registrationOpenDate(event.getRegistrationOpenDate())
                .registrationCloseDate(event.getRegistrationCloseDate())
                .capacity(event.getCapacity())
                .status(event.getStatus().name())
                .visibility(event.getVisibility().name())
                .paidSalesActivated(event.isPaidSalesActivated())
                .assignedSeating(event.isAssignedSeating())
                .createdAt(event.getCreatedAt())
                .build();
    }
}