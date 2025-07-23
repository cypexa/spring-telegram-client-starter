package com.cypexa.telegram.client.controller;

import com.cypexa.telegram.client.dto.AuthCodeRequestDto;
import com.cypexa.telegram.client.dto.AuthPhoneRequestDto;
import com.cypexa.telegram.client.dto.AuthResponseDto;
import com.cypexa.telegram.client.service.TelegramAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/telegram/auth")
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthController {
    
    private final TelegramAuthService authService;
    
    @PostMapping("/phone")
    public Mono<ResponseEntity<AuthResponseDto>> sendPhoneNumber(@RequestBody AuthPhoneRequestDto request) {
        log.info("Received phone number authorization request for: {}", request.getPhoneNumber());
        
        return authService.sendPhoneNumber(request.getPhoneNumber())
                .map(message -> {
                    String currentState = authService.getCurrentAuthState();
                    return ResponseEntity.ok(AuthResponseDto.success(currentState, message));
                });
    }
    
    @PostMapping("/code")
    public Mono<ResponseEntity<AuthResponseDto>> sendAuthCode(@RequestBody AuthCodeRequestDto request) {
        log.info("Received auth code verification request");
        
        return authService.sendAuthCode(request.getCode())
                .map(message -> {
                    String currentState = authService.getCurrentAuthState();
                    return ResponseEntity.ok(AuthResponseDto.success(currentState, message));
                });
    }
    
    @GetMapping("/status")
    public Mono<ResponseEntity<AuthResponseDto>> getAuthStatus() {
        return Mono.fromCallable(() -> {
            String currentState = authService.getCurrentAuthState();
            boolean isAuthorized = authService.isAuthorized();
            
            String message = isAuthorized ? "User is authorized" : "User is not authorized";
            
            return ResponseEntity.ok(AuthResponseDto.success(currentState, message));
        });
    }
} 