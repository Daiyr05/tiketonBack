package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.OrganizerProfile;
import org.example.lateticket.entity.User;
import org.example.lateticket.repository.OrganizerProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/organizer/profile")
@PreAuthorize("hasRole('ORGANIZER')")
@RequiredArgsConstructor
public class OrganizerProfileController {

    private final OrganizerProfileRepository profileRepository;

    @GetMapping
    public ResponseEntity<OrganizerProfile> getProfile(@AuthenticationPrincipal User user) {
        OrganizerProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found. Create one first."));
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<OrganizerProfile> createOrUpdateProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        OrganizerProfile profile = profileRepository.findByUserId(user.getId())
                .orElse(OrganizerProfile.builder().user(user).build());

        if (body.containsKey("organizationName")) profile.setOrganizationName(body.get("organizationName"));
        if (body.containsKey("description")) profile.setDescription(body.get("description"));
        if (body.containsKey("contactEmail")) profile.setContactEmail(body.get("contactEmail"));
        if (body.containsKey("contactPhone")) profile.setContactPhone(body.get("contactPhone"));
        if (body.containsKey("website")) profile.setWebsite(body.get("website"));
        if (body.containsKey("payoutBankName")) profile.setPayoutBankName(body.get("payoutBankName"));
        if (body.containsKey("payoutAccountNumber")) profile.setPayoutAccountNumber(body.get("payoutAccountNumber"));
        if (body.containsKey("payoutAccountHolder")) profile.setPayoutAccountHolder(body.get("payoutAccountHolder"));

        // For academic MVP: auto-verify
        profile.setIdentityVerified(true);
        profile.setPayoutVerified(true);

        return ResponseEntity.ok(profileRepository.save(profile));
    }
}