package com.cypexa.telegram.client.controller;

import com.cypexa.telegram.client.dto.MessageResponseDto;
import com.cypexa.telegram.client.dto.SendStickerRequestDto;
import com.cypexa.telegram.client.dto.StickerSetResponseDto;
import com.cypexa.telegram.client.service.TelegramStickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/telegram/stickers")
@RequiredArgsConstructor
@Slf4j
public class TelegramStickerController {

    private final TelegramStickerService stickerService;

    @GetMapping("/sets")
    public Mono<ResponseEntity<List<StickerSetResponseDto>>> getInstalledStickerSets() {
        log.info("Received request to get installed sticker sets");
        
        return stickerService.getInstalledStickerSets()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/sets/{stickerSetName}")
    public Mono<ResponseEntity<StickerSetResponseDto>> getStickerSet(
            @PathVariable String stickerSetName) {
        log.info("Received request to get sticker set: {}", stickerSetName);
        
        return stickerService.getStickerSet(stickerSetName)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/send")
    public Mono<ResponseEntity<MessageResponseDto>> sendStickerToChat(
            @RequestBody SendStickerRequestDto request) {
        log.info("Received request to send sticker to chat: {}", request.getChatId());
        
        // Устанавливаем chatId из path parameter
        request.setChatId(request.getChatId());
        
        return stickerService.sendSticker(request)
                .map(ResponseEntity::ok);
    }
} 