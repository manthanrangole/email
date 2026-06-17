package com.mailer.service;

import com.mailer.dto.BulkEmailResponse;
import com.mailer.model.EmailRequest;
import com.mailer.model.HrContact;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core email service. Reads the email body from:
 *   src/main/resources/templates/email-body.html
 *
 * To change the email content, edit that HTML file.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;
    private final EmailJsonLoaderService jsonLoaderService;

    @Value("${app.cv-path}")
    private String cvPath;

    @Value("${app.email.subject}")
    private String emailSubject;

    @Value("${app.email.sender-name}")
    private String senderName;

    @Value("${app.email.sender-email}")
    private String senderEmail;

    @Value("${app.email.delay-between-emails-ms:1000}")
    private long delayBetweenEmailsMs;

    // Shared state for /status endpoint
    private final AtomicReference<BulkEmailResponse> lastResponse = new AtomicReference<>();

    // -----------------------------------------------------------------------
    // Send a single ad-hoc email
    // -----------------------------------------------------------------------
    public void sendSingleEmail(String email) throws MessagingException, IOException {
        String emailBody = loadEmailTemplate();
        log.info("Sending single email to {}", email);
        sendEmail(email, emailBody);
    }

    // -----------------------------------------------------------------------
    // Async entry point — API returns immediately, emails send in background
    // -----------------------------------------------------------------------

    @Async("emailTaskExecutor")
    public void sendBulkEmailsAsync(List<HrContact> contacts) {
        BulkEmailResponse result = sendBulkEmails(contacts);
        lastResponse.set(result);
    }

    // -----------------------------------------------------------------------
    // Synchronous bulk send — returns full result summary
    // -----------------------------------------------------------------------

    public BulkEmailResponse sendBulkEmails(List<HrContact> contacts) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<String> failedEmails = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // Load email body template once
        String emailBody;
        try {
            emailBody = loadEmailTemplate();
        } catch (IOException e) {
            log.error("Cannot read email-body.html template: {}", e.getMessage());
            return BulkEmailResponse.builder()
                    .totalEmails(contacts.size())
                    .successCount(0)
                    .failedCount(contacts.size())
                    .status("TEMPLATE_NOT_FOUND")
                    .build();
        }

        log.info("=== Starting bulk email send to {} contacts ===", contacts.size());

        for (HrContact contact : contacts) {
            // Validate email format
            if (!jsonLoaderService.isValidEmail(contact.getEmail())) {
                log.warn("Skipping invalid email: {}", contact.getEmail());
                failedEmails.add(contact.getEmail() + " (invalid format)");
                continue;
            }

            try {
                sendEmail(contact.getEmail(), emailBody);
                successCount.incrementAndGet();
                log.info("[✓] Sent to {} at {}",
                        contact.getEmail(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                // Rate limiting — avoids Gmail spam flags
                Thread.sleep(delayBetweenEmailsMs);

            } catch (MessagingException | IOException e) {
                log.error("[✗] Failed to send to {}: {}", contact.getEmail(), e.getMessage());
                failedEmails.add(contact.getEmail());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Email sending interrupted");
                failedEmails.add(contact.getEmail());
            }
        }

        LocalDateTime completedAt = LocalDateTime.now();
        String timeTaken = String.format("%.1fs",
                Duration.between(startedAt, completedAt).toMillis() / 1000.0);

        String status = failedEmails.isEmpty() ? "ALL_SUCCESS"
                : (successCount.get() == 0 ? "ALL_FAILED" : "PARTIAL_SUCCESS");

        BulkEmailResponse response = BulkEmailResponse.builder()
                .totalEmails(contacts.size())
                .successCount(successCount.get())
                .failedCount(failedEmails.size())
                .failedEmails(failedEmails)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .timeTaken(timeTaken)
                .status(status)
                .build();

        log.info("=== Done: {}/{} succeeded in {} ===",
                successCount.get(), contacts.size(), timeTaken);

        lastResponse.set(response);
        return response;
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    /**
     * Sends a single MimeMessage to one email address with CV attached.
     */
    private void sendEmail(String toEmail, String htmlBody) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail, senderName);
        helper.setTo(toEmail);
        helper.setSubject(emailSubject);
        helper.setText(htmlBody, true); // true = HTML

        // Attach CV
        Resource cvResource = resourceLoader.getResource(cvPath);
        if (!cvResource.exists()) {
            log.warn("CV not found at {}. Sending without attachment.", cvPath);
        } else {
            String filename = cvResource.getFilename();
            if (filename == null) {
                filename = "Resume.pdf";
            }
            helper.addAttachment(filename, cvResource);
        }

        mailSender.send(message);
    }

    /**
     * Loads the email body from:
     *   src/main/resources/templates/email-body.html
     *
     * ✏️  Edit that file to change the email content.
     */
    private String loadEmailTemplate() throws IOException {
        Resource templateResource = resourceLoader.getResource("classpath:templates/email-body.html");
        try (InputStream is = templateResource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Returns the result of the last bulk send (used by /status endpoint). */
    public BulkEmailResponse getLastResponse() {
        return lastResponse.get();
    }

}
