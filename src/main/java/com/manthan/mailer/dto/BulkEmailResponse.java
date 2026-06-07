package com.manthan.mailer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO returned after a bulk email send operation.
 */
@Data
@Builder
public class BulkEmailResponse {

    private int totalEmails;
    private int successCount;
    private int failedCount;
    private List<String> failedEmails;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String timeTaken;
    private String status;

}
