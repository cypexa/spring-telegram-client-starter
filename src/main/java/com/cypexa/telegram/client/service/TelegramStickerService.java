package com.cypexa.telegram.client.service;

import com.cypexa.telegram.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.TdApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TelegramStickerService {

    private final TelegramAuthService authService;
    
    @Autowired
    public TelegramStickerService(TelegramAuthService authService) {
        this.authService = authService;
    }

    public Mono<List<StickerSetResponseDto>> getInstalledStickerSets() {
        return Mono.<List<StickerSetResponseDto>>create(sink -> {
            if (!authService.isAuthorized()) {
                sink.error(new RuntimeException("Not authorized"));
                return;
            }

            // Получаем установленные наборы стикеров
            TdApi.GetInstalledStickerSets getInstalledSets = new TdApi.GetInstalledStickerSets(
                new TdApi.StickerTypeRegular()
            );
            
            authService.getClient().send(getInstalledSets, result -> {
                if (result instanceof TdApi.StickerSets stickerSets) {
                    List<StickerSetResponseDto> response = convertStickerSetsToDto(stickerSets);
                    sink.success(response);
                } else if (result instanceof TdApi.Error error) {
                    sink.error(new RuntimeException("Error getting sticker sets: " + error.message));
                } else {
                    sink.error(new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName()));
                }
            });
        })
        .doOnSubscribe(subscription -> log.info("Getting installed sticker sets"))
        .doOnSuccess(result -> log.info("Successfully got {} sticker sets", result.size()))
        .doOnError(error -> log.error("Error getting sticker sets", error));
    }

    public Mono<StickerSetResponseDto> getStickerSet(String name) {
        return Mono.<StickerSetResponseDto>create(sink -> {
            if (!authService.isAuthorized()) {
                sink.error(new RuntimeException("Not authorized"));
                return;
            }

            TdApi.GetStickerSet getStickerSet = new TdApi.GetStickerSet(Long.parseLong(name));
            
            authService.getClient().send(getStickerSet, result -> {
                if (result instanceof TdApi.StickerSet stickerSet) {
                    StickerSetResponseDto response = convertStickerSetToDto(stickerSet);
                    sink.success(response);
                } else if (result instanceof TdApi.Error error) {
                    sink.error(new RuntimeException("Error getting sticker set: " + error.message));
                } else {
                    sink.error(new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName()));
                }
            });
        })
        .doOnSubscribe(subscription -> log.info("Getting sticker set: {}", name))
        .doOnSuccess(result -> log.info("Successfully got sticker set: {}", result.getTitle()))
        .doOnError(error -> log.error("Error getting sticker set: {}", name, error));
    }

    public Mono<MessageResponseDto> sendSticker(SendStickerRequestDto request) {
        return Mono.<MessageResponseDto>create(sink -> {
            if (!authService.isAuthorized()) {
                sink.error(new RuntimeException("Not authorized"));
                return;
            }

            // Создаем стикер сообщение
            TdApi.InputMessageSticker inputMessageSticker = new TdApi.InputMessageSticker();
            
            // Создаем InputFile из fileId или stickerId
            if (request.getStickerFileId() != null && !request.getStickerFileId().isEmpty()) {
                inputMessageSticker.sticker = new TdApi.InputFileId(Integer.parseInt(request.getStickerFileId()));
            } else {
                inputMessageSticker.sticker = new TdApi.InputFileId(request.getStickerId());
            }
            
            inputMessageSticker.width = 0; // Будет установлено автоматически
            inputMessageSticker.height = 0; // Будет установлено автоматически

            // Создаем reply-to если указано
            TdApi.InputMessageReplyTo replyTo = null;
            if (request.getReplyToMessageId() > 0) {
                replyTo = new TdApi.InputMessageReplyToMessage();
                ((TdApi.InputMessageReplyToMessage) replyTo).messageId = request.getReplyToMessageId();
            }

            // Создаем опции отправки
            TdApi.MessageSendOptions options = new TdApi.MessageSendOptions();
            options.disableNotification = request.isDisableNotification();

            TdApi.SendMessage sendMessage = new TdApi.SendMessage(
                request.getChatId(),
                0, // messageThreadId
                replyTo,
                options,
                null, // replyMarkup
                inputMessageSticker
            );

            authService.getClient().send(sendMessage, result -> {
                if (result instanceof TdApi.Message message) {
                    MessageResponseDto response = MessageResponseDto.success(
                        message.id,
                        message.chatId,
                        "Sticker",
                        message.date
                    );
                    sink.success(response);
                } else if (result instanceof TdApi.Error error) {
                    sink.error(new RuntimeException("Error sending sticker: " + error.message));
                } else {
                    sink.error(new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName()));
                }
            });
        })
        .doOnSubscribe(subscription -> log.info("Sending sticker to chat: {}", request.getChatId()))
        .doOnSuccess(result -> log.info("Successfully sent sticker with message ID: {}", result.getMessageId()))
        .doOnError(error -> log.error("Error sending sticker to chat: {}", request.getChatId(), error));
    }

    private List<StickerSetResponseDto> convertStickerSetsToDto(TdApi.StickerSets stickerSets) {
        List<StickerSetResponseDto> result = new ArrayList<>();
        for (TdApi.StickerSetInfo setInfo : stickerSets.sets) {
            StickerSetResponseDto dto = StickerSetResponseDto.success(
                setInfo.id,
                setInfo.name,
                setInfo.title
            );
            dto.setInstalled(setInfo.isInstalled);
            dto.setOfficial(setInfo.isOfficial);
            dto.setAnimated(false);
            dto.setVideo(false);
            dto.setStickerCount(setInfo.size);
            result.add(dto);
        }
        return result;
    }

    private StickerSetResponseDto convertStickerSetToDto(TdApi.StickerSet stickerSet) {
        StickerSetResponseDto dto = StickerSetResponseDto.success(
            stickerSet.id,
            stickerSet.name,
            stickerSet.title
        );
        dto.setInstalled(stickerSet.isInstalled);
        dto.setOfficial(stickerSet.isOfficial);
        dto.setAnimated(false);
        dto.setVideo(false);
        dto.setStickerCount(stickerSet.stickers.length);

        // Конвертируем стикеры
        List<StickerResponseDto> stickers = new ArrayList<>();
        for (TdApi.Sticker sticker : stickerSet.stickers) {
            StickerResponseDto stickerDto = new StickerResponseDto();
            stickerDto.setFileId(sticker.sticker.id);
            stickerDto.setEmoji(sticker.emoji);
            stickerDto.setSetName(stickerSet.name);
            stickerDto.setAnimated(false);
            stickerDto.setVideo(false);
            stickerDto.setWidth(sticker.width);
            stickerDto.setHeight(sticker.height);
            
            if (sticker.thumbnail != null) {
                stickerDto.setThumbnailUrl(sticker.thumbnail.file.local.path);
            }
            
            stickers.add(stickerDto);
        }
        dto.setStickers(stickers);

        return dto;
    }
} 