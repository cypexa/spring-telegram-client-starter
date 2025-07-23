package com.cypexa.telegram.client.dto;

import lombok.Data;

@Data
public class SendStickerRequestDto {
    private long chatId;
    private int stickerId;
    private String stickerFileId;
    private long replyToMessageId;
    private boolean disableNotification;
} 