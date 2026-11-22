package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.dto.user.UserResponse;
import org.example.lateticket.entity.User;
import org.example.lateticket.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** The signed-in user's own profile. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Find a user by exact email, e.g. to assign them as event staff. */
    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'PLATFORM_ADMIN')")
    public ResponseEntity<UserResponse> lookup(@RequestParam String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .or(() -> userRepository.findByEmail(email.trim()))
                .orElseThrow(() -> new RuntimeException("No user with email " + email));
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
