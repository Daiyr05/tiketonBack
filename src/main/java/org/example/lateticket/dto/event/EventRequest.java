package org.example.lateticket.dto.event;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {
    @NotBlank
    private String title;
    private String description;
    private String category;
    private String imageUrl;
    private String venueName;
    private String venueAddress;
    private String city;
    private LocalDateTime eventDate;
    private LocalDateTime eventEndDate;
    private LocalDateTime registrationOpenDate;
    private LocalDateTime registrationCloseDate;
    private Integer capacity;
    private String visibility; // PUBLIC, UNLISTED, PRIVATE
    private boolean assignedSeating;
    private String refundPolicy;
}