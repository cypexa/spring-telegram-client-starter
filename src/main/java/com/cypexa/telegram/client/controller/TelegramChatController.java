package com.cypexa.telegram.client.controller;

import com.cypexa.telegram.client.dto.*;
import com.cypexa.telegram.client.service.TelegramChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/telegram/chats")
@RequiredArgsConstructor
@Slf4j
public class TelegramChatController {

    private final TelegramChatService chatService;

    @GetMapping
    public Mono<ResponseEntity<ChatListResponseDto>> getChats(
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Received request to get chats with limit: {}", limit);
        
        return chatService.getChats(limit)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error getting chats", ex);
                    return Mono.just(ResponseEntity.badRequest().body(
                            ChatListResponseDto.error(ex.getMessage())
                    ));
                });
    }

    @GetMapping("/{chatId}")
    public Mono<ResponseEntity<ChatResponseDto>> getChatById(@PathVariable long chatId) {
        log.info("Received request to get chat by ID: {}", chatId);
        
        return chatService.getChatById(chatId)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error getting chat by ID: {}", chatId, ex);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    @PostMapping("/{chatId}/messages")
    public Mono<ResponseEntity<MessageResponseDto>> sendMessage(
            @PathVariable long chatId,
            @RequestBody SendMessageRequestDto request) {
        log.info("Received request to send message to chat: {}", chatId);
        
        // Устанавливаем chatId из path parameter
        request.setChatId(chatId);
        
        return chatService.sendMessage(request)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error sending message to chat: {}", chatId, ex);
                    return Mono.just(ResponseEntity.badRequest().body(
                            MessageResponseDto.error(ex.getMessage())
                    ));
                });
    }

    @PostMapping("/messages")
    public Mono<ResponseEntity<MessageResponseDto>> sendMessageToChat(
            @RequestBody SendMessageRequestDto request) {
        log.info("Received request to send message to chat: {}", request.getChatId());
        
        return chatService.sendMessage(request)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error sending message", ex);
                    return Mono.just(ResponseEntity.badRequest().body(
                            MessageResponseDto.error(ex.getMessage())
                    ));
                });
    }
} 