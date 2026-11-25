package org.example.lateticket.dto.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventResponse {
    private Long id;
    private Long organizerId;
    private String organizerName;
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
    private String status;
    private String visibility;
    private boolean paidSalesActivated;
    private boolean assignedSeating;
    private String refundPolicy;
    private LocalDateTime createdAt;
}