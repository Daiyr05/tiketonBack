package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Event;
import org.example.lateticket.repository.EventRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class CalendarController {

    private final EventRepository eventRepository;

    @GetMapping("/{id}/calendar")
    public ResponseEntity<byte[]> getCalendarFile(@PathVariable Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        DateTimeFormatter icsFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//BiletFlow//EN\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:event-").append(event.getId()).append("@biletflow\r\n");

        if (event.getEventDate() != null) {
            ics.append("DTSTART:").append(event.getEventDate().format(icsFormat)).append("\r\n");
        }
        if (event.getEventEndDate() != null) {
            ics.append("DTEND:").append(event.getEventEndDate().format(icsFormat)).append("\r\n");
        }

        ics.append("SUMMARY:").append(event.getTitle()).append("\r\n");
        if (event.getDescription() != null) {
            ics.append("DESCRIPTION:").append(event.getDescription().replace("\n", "\\n")).append("\r\n");
        }
        if (event.getVenueName() != null) {
            ics.append("LOCATION:").append(event.getVenueName());
            if (event.getVenueAddress() != null) {
                ics.append(", ").append(event.getVenueAddress());
            }
            ics.append("\r\n");
        }

        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");

        byte[] content = ics.toString().getBytes();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + event.getTitle().replaceAll("[^a-zA-Z0-9]", "_") + ".ics")
                .body(content);
    }
}