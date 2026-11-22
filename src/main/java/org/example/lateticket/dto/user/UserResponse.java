package org.example.lateticket.dto.user;

import lombok.Builder;
import lombok.Data;
import org.example.lateticket.entity.User;

/** Public view of a user: never carries password hashes or tokens. */
@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean emailVerified;
    private boolean suspended;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole().name())
                .emailVerified(u.isEmailVerified())
                .suspended(u.isSuspended())
                .build();
    }
}
