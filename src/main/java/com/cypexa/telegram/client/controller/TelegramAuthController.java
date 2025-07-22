package com.cypexa.telegram.client.controller;

import com.cypexa.telegram.client.dto.AuthCodeRequestDto;
import com.cypexa.telegram.client.dto.AuthPhoneRequestDto;
import com.cypexa.telegram.client.dto.AuthResponseDto;
import com.cypexa.telegram.client.service.TelegramAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/telegram/auth")
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthController {
    
    private final TelegramAuthService authService;
    
    @PostMapping("/phone")
    public CompletableFuture<ResponseEntity<AuthResponseDto>> sendPhoneNumber(@RequestBody AuthPhoneRequestDto request) {
        log.info("Received phone number authorization request for: {}", request.getPhoneNumber());
        
        return authService.sendPhoneNumber(request.getPhoneNumber())
                .thenApply(message -> {
                    String currentState = authService.getCurrentAuthState();
                    return ResponseEntity.ok(AuthResponseDto.success(currentState, message));
                })
                .exceptionally(ex -> {
                    log.error("Error sending phone number", ex);
                    return ResponseEntity.badRequest().body(
                            AuthResponseDto.error(ex.getMessage())
                    );
                });
    }
    
    @PostMapping("/code")
    public CompletableFuture<ResponseEntity<AuthResponseDto>> sendAuthCode(@RequestBody AuthCodeRequestDto request) {
        log.info("Received auth code verification request");
        
        return authService.sendAuthCode(request.getCode())
                .thenApply(message -> {
                    String currentState = authService.getCurrentAuthState();
                    return ResponseEntity.ok(AuthResponseDto.success(currentState, message));
                })
                .exceptionally(ex -> {
                    log.error("Error verifying auth code", ex);
                    return ResponseEntity.badRequest().body(
                            AuthResponseDto.error(ex.getMessage())
                    );
                });
    }
    
    @GetMapping("/status")
    public ResponseEntity<AuthResponseDto> getAuthStatus() {
        String currentState = authService.getCurrentAuthState();
        boolean isAuthorized = authService.isAuthorized();
        
        String message = isAuthorized ? "User is authorized" : "User is not authorized";
        
        return ResponseEntity.ok(AuthResponseDto.success(currentState, message));
    }
} 