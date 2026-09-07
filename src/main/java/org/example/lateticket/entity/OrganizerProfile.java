package org.example.lateticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organizer_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String organizationName;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String website;

    // Payout info (simulated for academic MVP)
    private String payoutBankName;
    private String payoutAccountNumber;
    private String payoutAccountHolder;
    private boolean identityVerified;
    private boolean payoutVerified;
}