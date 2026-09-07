package org.example.lateticket.repository;

import org.example.lateticket.entity.Event;
import org.example.lateticket.entity.enums.EventStatus;
import org.example.lateticket.entity.enums.EventVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizerId(Long organizerId);
    List<Event> findByOrganizerIdAndStatus(Long organizerId, EventStatus status);
    List<Event> findByStatusAndVisibility(EventStatus status, EventVisibility visibility);
    List<Event> findByStatus(EventStatus status);
}