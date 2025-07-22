package com.cypexa.telegram.client.dto;

import lombok.Data;

@Data
public class AuthResponseDto {
    private String state;
    private String message;
    private boolean success;
    private String error;
    
    public static AuthResponseDto success(String state, String message) {
        AuthResponseDto response = new AuthResponseDto();
        response.setState(state);
        response.setMessage(message);
        response.setSuccess(true);
        return response;
    }
    
    public static AuthResponseDto error(String error) {
        AuthResponseDto response = new AuthResponseDto();
        response.setError(error);
        response.setSuccess(false);
        return response;
    }
} 