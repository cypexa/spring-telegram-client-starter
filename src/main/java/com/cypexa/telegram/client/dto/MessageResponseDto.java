package com.cypexa.telegram.client.dto;

import lombok.Data;

@Data
public class MessageResponseDto {
    private long messageId;
    private long chatId;
    private String text;
    private long date;
    private boolean success;
    private String error;
    
    public static MessageResponseDto success(long messageId, long chatId, String text, long date) {
        MessageResponseDto response = new MessageResponseDto();
        response.setMessageId(messageId);
        response.setChatId(chatId);
        response.setText(text);
        response.setDate(date);
        response.setSuccess(true);
        return response;
    }
    
    public static MessageResponseDto error(String error) {
        MessageResponseDto response = new MessageResponseDto();
        response.setError(error);
        response.setSuccess(false);
        return response;
    }
} 