package com.email.writer.app;

import lombok.Data;

@Data
public class EmailRequest {
    private String subject;       // Subject of the email
    private String emailContent;  // Body of the email
    private String tone;          // Desired tone (formal, casual, etc.)
}
