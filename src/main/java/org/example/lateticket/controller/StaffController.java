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

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<StaffAssignment> assignStaff(
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal User organizer) {
        Long eventId = body.get("eventId");
        Long userId = body.get("userId");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        User staff = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (staffAssignmentRepository.existsByEventIdAndUserId(eventId, userId)) {
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
    public ResponseEntity<Void> removeStaff(@PathVariable Long id) {
        staffAssignmentRepository.deleteById(id);
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