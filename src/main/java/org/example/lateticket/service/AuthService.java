package org.example.lateticket.service;

import lombok.extern.slf4j.Slf4j;
import org.example.lateticket.dto.auth.*;
import org.example.lateticket.entity.User;
import org.example.lateticket.entity.enums.Role;
import org.example.lateticket.repository.UserRepository;
import org.example.lateticket.security.JwtUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    static final int RESET_TOKEN_MINUTES = 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean exposeResetToken;
    private final String frontendUrl;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.auth.expose-reset-token:false}") boolean exposeResetToken,
                       @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
        this.exposeResetToken = exposeResetToken;
        this.frontendUrl = frontendUrl;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Role role = Role.ATTENDEE;
        if (request.getRole() != null && request.getRole().equalsIgnoreCase("ORGANIZER")) {
            role = Role.ORGANIZER;
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(role)
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .suspended(false)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(),
                user.getRole().name(), user.getFirstName(), user.getLastName());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (user.isSuspended()) {
            throw new RuntimeException("Account is suspended");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(),
                user.getRole().name(), user.getFirstName(), user.getLastName());
    }

    public void verifyEmail(String verificationToken) {
        User user = userRepository.findByVerificationToken(verificationToken)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    /**
     * Starts a password reset. Always answers the same way whether or not the email exists,
     * so the endpoint cannot be used to enumerate accounts.
     */
    public Map<String, Object> forgotPassword(String email) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "If an account with that email exists, a reset link has been sent");

        userRepository.findByEmail(email.trim()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
            userRepository.save(user);

            String link = frontendUrl + "/reset-password?token=" + token;
            sendResetEmail(user.getEmail(), link);

            if (exposeResetToken) {
                // Academic MVP without a mail provider: hand the token back so the flow can be tested
                result.put("resetToken", token);
                result.put("expiresInMinutes", RESET_TOKEN_MINUTES);
            }
        });
        return result;
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetPasswordExpiry() == null || user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            user.setResetPasswordToken(null);
            user.setResetPasswordExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);
        userRepository.save(user);
    }

    private void sendResetEmail(String to, String link) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("Mail sender not configured; password reset link for {}: {}", to, link);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject("BiletFlow: reset your password");
            msg.setText("Open this link to choose a new password (valid for " + RESET_TOKEN_MINUTES
                    + " minutes):\n" + link);
            sender.send(msg);
        } catch (Exception e) {
            // SMTP is a placeholder in the academic MVP; don't fail the request over it
            log.warn("Could not send password reset email to {}: {}", to, e.getMessage());
            log.info("Password reset link for {}: {}", to, link);
        }
    }
}
