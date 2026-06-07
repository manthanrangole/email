package com.manthan.mailer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an HR contact — only the email address is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrContact {

    private String email;

}
