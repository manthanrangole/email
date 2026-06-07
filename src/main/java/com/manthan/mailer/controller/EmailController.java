package com.manthan.mailer.controller;

import com.manthan.mailer.dto.BulkEmailResponse;
import com.manthan.mailer.model.EmailRequest;
import com.manthan.mailer.model.HrContact;
import com.manthan.mailer.service.EmailJsonLoaderService;
import jakarta.validation.Valid;
import com.manthan.mailer.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for job application email endpoints.
 */
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Controller", description = "APIs for sending job application emails in bulk")
public class EmailController {

    private final EmailService emailService;
    private final EmailJsonLoaderService jsonLoaderService;
    private final ResourceLoader resourceLoader;

    @Value("${app.cv-path}")
    private String cvPath;

    // -------------------------------------------------------------------
    // POST /api/email/send-single
    // -------------------------------------------------------------------
    @PostMapping("/send-single")
    @Operation(
        summary = "Send an email to a single recipient",
        description = "Sends a standard email to one recipient specified in the request body, with the CV attached.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request details"),
            @ApiResponse(responseCode = "500", description = "Failed to send email due to messaging or configuration error")
        }
    )
    public ResponseEntity<Map<String, String>> sendSingle(@RequestBody HrContact contact) {
        if (contact == null || !jsonLoaderService.isValidEmail(contact.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", "Invalid or blank recipient email"
            ));
        }
        log.info("Received request to send single email to: {}", contact.getEmail());
        try {
            emailService.sendSingleEmail(contact.getEmail());
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Email successfully sent to " + contact.getEmail()
            ));
        } catch (Exception e) {
            log.error("Failed to send single email to {}: {}", contact.getEmail(), e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAILED",
                    "message", "Failed to send email: " + e.getMessage()
            ));
        }
    }

    // -------------------------------------------------------------------
    // POST /api/email/upload-and-send
    // -------------------------------------------------------------------
    @PostMapping(value = "/upload-and-send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload JSON file and send emails synchronously",
        description = "Upload an hr_emails.json file containing HR contacts. Emails are sent synchronously and the full result is returned.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bulk email result",
                content = @Content(schema = @Schema(implementation = BulkEmailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or JSON format")
        }
    )
    public ResponseEntity<BulkEmailResponse> uploadAndSend(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<HrContact> contacts = jsonLoaderService.loadFromMultipartFile(file);
            if (contacts.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(BulkEmailResponse.builder()
                                .totalEmails(0)
                                .status("NO_CONTACTS_FOUND")
                                .build());
            }
            log.info("Received upload-and-send request with {} contacts", contacts.size());
            BulkEmailResponse response = emailService.sendBulkEmails(contacts);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to parse uploaded JSON file: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // -------------------------------------------------------------------
    // POST /api/email/upload-and-send-async
    // -------------------------------------------------------------------
    @PostMapping(value = "/upload-and-send-async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload JSON file and send emails asynchronously",
        description = "Upload an hr_emails.json file. Emails are queued and sent in the background. API returns immediately. Poll /status for results.",
        responses = {
            @ApiResponse(responseCode = "202", description = "Accepted — emails sending in background"),
            @ApiResponse(responseCode = "400", description = "Invalid file or JSON format")
        }
    )
    public ResponseEntity<Map<String, Object>> uploadAndSendAsync(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<HrContact> contacts = jsonLoaderService.loadFromMultipartFile(file);
            if (contacts.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No contacts found in the uploaded file"));
            }
            log.info("Received async upload-and-send request with {} contacts", contacts.size());
            emailService.sendBulkEmailsAsync(contacts);
            return ResponseEntity.accepted()
                    .body(Map.of(
                            "message", "Email sending started in background",
                            "totalContacts", contacts.size(),
                            "statusEndpoint", "/api/email/status"
                    ));

        } catch (IOException e) {
            log.error("Failed to parse uploaded JSON file: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid JSON file: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------
    // POST /api/email/send-from-file
    // -------------------------------------------------------------------
    @PostMapping("/send-from-file")
    @Operation(
        summary = "Send emails using pre-placed hr_emails.json in resources",
        description = "Uses the hr_emails.json file already placed in src/main/resources/data/. Sends synchronously.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bulk email result"),
            @ApiResponse(responseCode = "404", description = "hr_emails.json not found in resources")
        }
    )
    public ResponseEntity<BulkEmailResponse> sendFromPreplacedFile() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data/hr_emails.json");
            if (!resource.exists()) {
                return ResponseEntity.status(404)
                        .body(BulkEmailResponse.builder()
                                .status("FILE_NOT_FOUND: Place hr_emails.json in src/main/resources/data/")
                                .build());
            }

            List<HrContact> contacts = jsonLoaderService.loadFromFile(resource.getFile());
            log.info("Loaded {} contacts from classpath:data/hr_emails.json", contacts.size());
            BulkEmailResponse response = emailService.sendBulkEmails(contacts);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to load pre-placed JSON file: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(BulkEmailResponse.builder()
                            .status("ERROR: " + e.getMessage())
                            .build());
        }
    }

    // -------------------------------------------------------------------
    // GET /api/email/status
    // -------------------------------------------------------------------
    @GetMapping("/status")
    @Operation(
        summary = "Get status of last bulk email send",
        description = "Returns the result of the most recent bulk email send operation.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Last bulk email result"),
            @ApiResponse(responseCode = "204", description = "No send operation has been performed yet")
        }
    )
    public ResponseEntity<BulkEmailResponse> getStatus() {
        BulkEmailResponse lastResponse = emailService.getLastResponse();
        if (lastResponse == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lastResponse);
    }

    // -------------------------------------------------------------------
    // GET /api/email/health
    // -------------------------------------------------------------------
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is up and CV is accessible")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Resource cvResource = resourceLoader.getResource(cvPath);
        boolean cvFound = cvResource.exists();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "cvAttachmentReady", cvFound,
                "cvPath", cvPath,
                "message", cvFound
                        ? "Service is ready to send emails"
                        : "⚠️ CV file not found — place your PDF at src/main/resources/cv/Manthan_Rangole_CV.pdf"
        ));
    }

}
