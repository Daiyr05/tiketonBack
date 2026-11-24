package org.example.lateticket.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String role;
    private String firstName;
    private String lastName;

    /** Only returned by register while app.auth.expose-verification-token=true (demo without SMTP). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String verificationToken;

    public AuthResponse(String token, Long userId, String email, String role, String firstName, String lastName) {
        this(token, userId, email, role, firstName, lastName, null);
    }
}