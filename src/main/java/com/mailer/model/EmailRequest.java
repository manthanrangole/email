package com.mailer.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Optional request body for ad-hoc single email requests.
 */
@Data
public class EmailRequest {

    @NotBlank(message = "Recipient name must not be blank")
    private String recipientName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Recipient email must not be blank")
    private String recipientEmail;

    @NotBlank(message = "Company name must not be blank")
    private String company;

}
