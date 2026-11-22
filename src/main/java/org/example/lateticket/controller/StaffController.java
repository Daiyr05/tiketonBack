package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.*;
import org.example.lateticket.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffAssignmentRepository staffAssignmentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    /** Assign a user as event staff. Body: { "eventId": 1, "userId": 4 } or { "eventId": 1, "email": "x@y.kz" }. */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<StaffAssignment> assignStaff(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User organizer) {
        if (body.get("eventId") == null) {
            throw new RuntimeException("eventId is required");
        }
        Long eventId = Long.valueOf(body.get("eventId").toString());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        User staff;
        if (body.get("userId") != null) {
            staff = userRepository.findById(Long.valueOf(body.get("userId").toString()))
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else if (body.get("email") != null) {
            String email = body.get("email").toString().trim();
            staff = userRepository.findByEmail(email.toLowerCase())
                    .or(() -> userRepository.findByEmail(email))
                    .orElseThrow(() -> new RuntimeException("No user with email " + email));
        } else {
            throw new RuntimeException("Either userId or email is required");
        }

        if (staffAssignmentRepository.existsByEventIdAndUserId(eventId, staff.getId())) {
            throw new RuntimeException("User already assigned");
        }

        StaffAssignment assignment = StaffAssignment.builder()
                .event(event)
                .user(staff)
                .build();

        return ResponseEntity.ok(staffAssignmentRepository.save(assignment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> removeStaff(@PathVariable Long id, @AuthenticationPrincipal User organizer) {
        StaffAssignment assignment = staffAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        if (!assignment.getEvent().getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }
        staffAssignmentRepository.delete(assignment);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<StaffAssignment>> getEventStaff(@PathVariable Long eventId) {
        return ResponseEntity.ok(staffAssignmentRepository.findByEventId(eventId));
    }

    @GetMapping("/my-events")
    public ResponseEntity<List<StaffAssignment>> getMyAssignments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(staffAssignmentRepository.findByUserId(user.getId()));
    }
}
