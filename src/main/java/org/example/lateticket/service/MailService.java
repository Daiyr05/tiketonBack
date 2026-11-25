package org.example.lateticket.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Sends plain-text emails. Never fails the caller: with app.mail.enabled=false (default) or a broken
 * SMTP config the email is only written to the log.
 */
@Service
@Slf4j
public class MailService {

    public record Attachment(String filename, String contentType, byte[] content) {}

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.mail.enabled:false}") boolean enabled,
                       @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    /** True when emails are really sent; callers can skip expensive work (e.g. PDFs) otherwise. */
    public boolean isEnabled() {
        return enabled && mailSender.getIfAvailable() != null;
    }

    public void send(String to, String subject, String text) {
        send(to, subject, text, List.of());
    }

    public void send(String to, String subject, String text, List<Attachment> attachments) {
        JavaMailSender sender = enabled ? mailSender.getIfAvailable() : null;
        if (sender == null) {
            log.info("Mail disabled; email \"{}\" for {}:\n{}", subject, to, text);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, !attachments.isEmpty(), "UTF-8");
            if (from != null && !from.isBlank()) helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            for (Attachment a : attachments) {
                helper.addAttachment(a.filename(), new ByteArrayResource(a.content()), a.contentType());
            }
            sender.send(message);
        } catch (Exception e) {
            log.warn("Could not send email \"{}\" to {}: {}", subject, to, e.getMessage());
            log.info("Email \"{}\" for {}:\n{}", subject, to, text);
        }
    }

    /** Sends after the current transaction commits (so a rollback never leaves a sent email behind). */
    public void sendAfterCommit(String to, String subject, String text, List<Attachment> attachments) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(to, subject, text, attachments);
                }
            });
        } else {
            send(to, subject, text, attachments);
        }
    }
}
