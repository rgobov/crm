package com.tryneuro.backend.dto;

import lombok.Data;

@Data
public class SendReturnReminderRequest {
    private String contactId;
    private String message;
}
