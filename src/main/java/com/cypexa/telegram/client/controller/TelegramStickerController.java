package com.cypexa.telegram.client.controller;

import com.cypexa.telegram.client.dto.*;
import com.cypexa.telegram.client.service.TelegramStickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/telegram/stickers")
@RequiredArgsConstructor
@Slf4j
public class TelegramStickerController {

    private final TelegramStickerService stickerService;

    @GetMapping("/sets")
    public Mono<ResponseEntity<List<StickerSetResponseDto>>> getInstalledStickerSets() {
        log.info("Received request to get installed sticker sets");
        
        return stickerService.getInstalledStickerSets()
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error getting sticker sets", ex);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    @GetMapping("/sets/{stickerSetName}")
    public Mono<ResponseEntity<StickerSetResponseDto>> getStickerSet(
            @PathVariable String stickerSetName) {
        log.info("Received request to get sticker set: {}", stickerSetName);
        
        return stickerService.getStickerSet(stickerSetName)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error getting sticker set: {}", stickerSetName, ex);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    @PostMapping("/send")
    public Mono<ResponseEntity<MessageResponseDto>> sendSticker(
            @RequestBody SendStickerRequestDto request) {
        log.info("Received request to send sticker to chat: {}", request.getChatId());
        
        return stickerService.sendSticker(request)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error sending sticker", ex);
                    return Mono.just(ResponseEntity.badRequest().body(
                            MessageResponseDto.error(ex.getMessage())
                    ));
                });
    }

    @PostMapping("/chats/{chatId}/stickers")
    public Mono<ResponseEntity<MessageResponseDto>> sendStickerToChat(
            @PathVariable long chatId,
            @RequestBody SendStickerRequestDto request) {
        log.info("Received request to send sticker to chat: {}", chatId);
        
        // Устанавливаем chatId из path parameter
        request.setChatId(chatId);
        
        return stickerService.sendSticker(request)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    log.error("Error sending sticker to chat: {}", chatId, ex);
                    return Mono.just(ResponseEntity.badRequest().body(
                            MessageResponseDto.error(ex.getMessage())
                    ));
                });
    }
} 