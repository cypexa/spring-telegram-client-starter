package com.cypexa.telegram.client.dto;

import lombok.Data;

@Data
public class SendMessageRequestDto {
    private long chatId;
    private String text;
    private long replyToMessageId;
    private boolean disableNotification;
    private String parseMode;
} 