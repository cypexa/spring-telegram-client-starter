package com.cypexa.telegram.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatListResponseDto {
    private List<ChatResponseDto> chats;
    private int totalCount;
    private boolean success;
    private String error;
    
    public static ChatListResponseDto success(List<ChatResponseDto> chats, int totalCount) {
        ChatListResponseDto response = new ChatListResponseDto();
        response.setChats(chats);
        response.setTotalCount(totalCount);
        response.setSuccess(true);
        return response;
    }
    
    public static ChatListResponseDto error(String error) {
        ChatListResponseDto response = new ChatListResponseDto();
        response.setError(error);
        response.setSuccess(false);
        return response;
    }
} 