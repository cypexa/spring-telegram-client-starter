package com.cypexa.telegram.client.service;

import com.cypexa.telegram.client.dto.MessageResponseDto;
import com.cypexa.telegram.client.dto.SendStickerRequestDto;
import com.cypexa.telegram.client.dto.StickerResponseDto;
import com.cypexa.telegram.client.dto.StickerSetResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TelegramStickerService extends BaseTelegramService {
    
    @Autowired
    public TelegramStickerService(Client telegramClient, TelegramAuthService authService) {
        super(telegramClient, authService);
    }

    public Mono<List<StickerSetResponseDto>> getInstalledStickerSets() {
        return executeWithAuth("getInstalledStickerSets", sink -> {
            TdApi.GetInstalledStickerSets getInstalledSets = new TdApi.GetInstalledStickerSets(
                new TdApi.StickerTypeRegular()
            );

            sendTelegramRequest(getInstalledSets, sink, (result, s) -> {
                if (result instanceof TdApi.StickerSets stickerSets) {
                    List<StickerSetResponseDto> response = convertStickerSetsToDto(stickerSets);
                    s.success(response);
                } else {
                    throw new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName());
                }
            });
        });
    }

    public Mono<StickerSetResponseDto> getStickerSet(String name) {
        return executeWithAuth("getStickerSet", sink -> {
            TdApi.GetStickerSet getStickerSet = new TdApi.GetStickerSet(Long.parseLong(name));

            sendTelegramRequest(getStickerSet, sink, (result, s) -> {
                if (result instanceof TdApi.StickerSet stickerSet) {
                    StickerSetResponseDto response = convertStickerSetToDto(stickerSet);
                    s.success(response);
                } else {
                    throw new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName());
                }
            });
        });
    }

    public Mono<MessageResponseDto> sendSticker(SendStickerRequestDto request) {
        return executeWithAuth("sendSticker", sink -> {
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
            TdApi.InputMessageReplyToMessage replyTo = null;
            if (request.getReplyToMessageId() > 0) {
                replyTo = new TdApi.InputMessageReplyToMessage();
                replyTo.messageId = request.getReplyToMessageId();
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

            sendTelegramRequest(sendMessage, sink, (result, s) -> {
                if (result instanceof TdApi.Message message) {
                    MessageResponseDto response = MessageResponseDto.success(
                        message.id,
                        message.chatId,
                        "Sticker",
                        message.date
                    );
                    s.success(response);
                } else {
                    throw new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName());
                }
            });
        });
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