package org.example.lateticket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.lateticket.entity.enums.Role;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean emailVerified;

    @JsonIgnore
    private String verificationToken;

    @JsonIgnore
    private String resetPasswordToken;

    @JsonIgnore
    private LocalDateTime resetPasswordExpiry;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private boolean suspended;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}