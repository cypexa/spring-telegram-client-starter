package com.cypexa.telegram.client.controller;

import com.cypexa.telegram.client.dto.ChatListResponseDto;
import com.cypexa.telegram.client.dto.ChatResponseDto;
import com.cypexa.telegram.client.dto.MessageResponseDto;
import com.cypexa.telegram.client.dto.SendMessageRequestDto;
import com.cypexa.telegram.client.service.TelegramChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/telegram/chats")
@RequiredArgsConstructor
@Slf4j
public class TelegramChatController {

    private final TelegramChatService chatService;

    @GetMapping
    public Mono<ResponseEntity<ChatListResponseDto>> getChats(
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Received request to get chats with limit: {}", limit);
        
        return chatService.getChats(limit)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{chatId}")
    public Mono<ResponseEntity<ChatResponseDto>> getChatById(@PathVariable long chatId) {
        log.info("Received request to get chat by ID: {}", chatId);
        
        return chatService.getChatById(chatId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{chatId}/message")
    public Mono<ResponseEntity<MessageResponseDto>> sendMessageToChat(
            @PathVariable long chatId,
            @RequestBody SendMessageRequestDto request) {
        log.info("Received request to send message to chat: {}", chatId);
        
        // Устанавливаем chatId из path parameter
        request.setChatId(chatId);
        
        return chatService.sendMessage(request)
                .map(ResponseEntity::ok);
    }
} 