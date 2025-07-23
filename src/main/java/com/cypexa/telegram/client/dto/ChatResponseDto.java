package com.cypexa.telegram.client.dto;

import lombok.Data;

@Data
public class ChatResponseDto {
    private long id;
    private String title;
    private String type;
    private String description;
    private boolean isVerified;
    private boolean isChannel;
    private boolean isGroup;
    private int memberCount;
    private String photoUrl;
    private long lastMessageDate;
    private String lastMessageText;
    
    public static ChatResponseDto success(long id, String title, String type) {
        ChatResponseDto response = new ChatResponseDto();
        response.setId(id);
        response.setTitle(title);
        response.setType(type);
        return response;
    }
} 