package org.example.lateticket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.admin.EventReportResponse;
import org.example.lateticket.dto.admin.ReportRequest;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Any signed-in user can report an event to platform admins (SRS 4.12). */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventReportController {

    private final AdminService adminService;

    @PostMapping("/{id}/reports")
    public ResponseEntity<EventReportResponse> reportEvent(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(adminService.reportEvent(id, request.getReason(), user));
    }
}
