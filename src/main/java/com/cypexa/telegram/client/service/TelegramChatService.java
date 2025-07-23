package com.cypexa.telegram.client.service;

import com.cypexa.telegram.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class TelegramChatService {

    private final TelegramAuthService authService;
    
    // Локальное хранилище чатов как в примере
    private final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
    private final NavigableSet<OrderedChat> mainChatList = new TreeSet<>();
    private volatile boolean haveFullMainChatList = false;
    
    @Autowired
    public TelegramChatService(TelegramAuthService authService) {
        this.authService = authService;
    }

    public Mono<ChatListResponseDto> getChats(int limit) {
        return Mono.<ChatListResponseDto>create(sink -> {
            if (!authService.isAuthorized()) {
                sink.error(new RuntimeException("Not authorized"));
                return;
            }

            try {
                getMainChatList(limit, sink);
            } catch (Exception e) {
                sink.error(e);
            }
        })
        .doOnSubscribe(subscription -> log.info("Loading chats with limit: {}", limit))
        .doOnSuccess(result -> log.info("Successfully loaded {} chats", result.getTotalCount()))
        .doOnError(error -> log.error("Error loading chats", error));
    }

    private void getMainChatList(int limit, reactor.core.publisher.MonoSink<ChatListResponseDto> sink) {
        synchronized (mainChatList) {
            if (!haveFullMainChatList && limit > mainChatList.size()) {
                // Отправляем LoadChats запрос если есть неизвестные чаты
                authService.getClient().send(new TdApi.LoadChats(new TdApi.ChatListMain(), limit - mainChatList.size()), new Client.ResultHandler() {
                    @Override
                    public void onResult(TdApi.Object object) {
                        switch (object.getConstructor()) {
                            case TdApi.Error.CONSTRUCTOR:
                                if (((TdApi.Error) object).code == 404) {
                                    synchronized (mainChatList) {
                                        haveFullMainChatList = true;
                                    }
                                    // Повторяем запрос после установки флага
                                    getMainChatList(limit, sink);
                                } else {
                                    sink.error(new RuntimeException("LoadChats error: " + ((TdApi.Error) object).message));
                                }
                                break;
                            case TdApi.Ok.CONSTRUCTOR:
                                // Чаты уже получены через updates, повторяем запрос
                                getMainChatList(limit, sink);
                                break;
                            default:
                                sink.error(new RuntimeException("Wrong response from TDLib: " + object));
                        }
                    }
                });
                return;
            }

            // Возвращаем чаты из локального хранилища
            List<ChatResponseDto> chatList = new ArrayList<>();
            var iter = mainChatList.iterator();
            int count = Math.min(limit, mainChatList.size());
            
            for (int i = 0; i < count; i++) {
                long chatId = iter.next().chatId;
                TdApi.Chat chat = chats.get(chatId);
                if (chat != null) {
                    synchronized (chat) {
                        chatList.add(convertToDto(chat));
                    }
                }
            }
            
            sink.success(ChatListResponseDto.success(chatList, mainChatList.size()));
        }
    }

    public Mono<ChatResponseDto> getChatById(long chatId) {
        return Mono.<ChatResponseDto>create(sink -> {
            if (!authService.isAuthorized()) {
                sink.error(new RuntimeException("Not authorized"));
                return;
            }

            // Сначала проверяем локальное хранилище
            TdApi.Chat localChat = chats.get(chatId);
            if (localChat != null) {
                synchronized (localChat) {
                    sink.success(convertToDto(localChat));
                    return;
                }
            }

            // Если нет в локальном хранилище, запрашиваем
            TdApi.GetChat getChat = new TdApi.GetChat(chatId);
            authService.getClient().send(getChat, result -> {
                if (result instanceof TdApi.Chat chat) {
                    // Сохраняем в локальное хранилище
                    chats.put(chat.id, chat);
                    ChatResponseDto response = convertToDto(chat);
                    sink.success(response);
                } else if (result instanceof TdApi.Error error) {
                    sink.error(new RuntimeException("Error getting chat: " + error.message));
                } else {
                    sink.error(new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName()));
                }
            });
        })
        .doOnSubscribe(subscription -> log.info("Getting chat by ID: {}", chatId))
        .doOnSuccess(result -> log.info("Successfully got chat: {}", result.getTitle()))
        .doOnError(error -> log.error("Error getting chat by ID: {}", chatId, error));
    }

    public Mono<MessageResponseDto> sendMessage(SendMessageRequestDto request) {
        return Mono.<MessageResponseDto>create(sink -> {
            if (!authService.isAuthorized()) {
                sink.error(new RuntimeException("Not authorized"));
                return;
            }

            // Создаем текстовое сообщение
            TdApi.InputMessageText inputMessageText = new TdApi.InputMessageText();
            inputMessageText.text = new TdApi.FormattedText();
            inputMessageText.text.text = request.getText();
            inputMessageText.text.entities = new TdApi.TextEntity[0];

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
                inputMessageText
            );

            authService.getClient().send(sendMessage, result -> {
                if (result instanceof TdApi.Message message) {
                    MessageResponseDto response = MessageResponseDto.success(
                        message.id,
                        message.chatId,
                        extractMessageText(message),
                        message.date
                    );
                    sink.success(response);
                } else if (result instanceof TdApi.Error error) {
                    sink.error(new RuntimeException("Error sending message: " + error.message));
                } else {
                    sink.error(new RuntimeException("Unexpected response type: " + result.getClass().getSimpleName()));
                }
            });
        })
        .doOnSubscribe(subscription -> log.info("Sending message to chat: {}", request.getChatId()))
        .doOnSuccess(result -> log.info("Successfully sent message with ID: {}", result.getMessageId()))
        .doOnError(error -> log.error("Error sending message to chat: {}", request.getChatId(), error));
    }

    // Метод для обработки updates (нужно вызывать из TelegramAuthService)
    public void handleUpdate(TdApi.Object update) {
        switch (update.getConstructor()) {
            case TdApi.UpdateNewChat.CONSTRUCTOR: {
                TdApi.UpdateNewChat updateNewChat = (TdApi.UpdateNewChat) update;
                TdApi.Chat chat = updateNewChat.chat;
                synchronized (chat) {
                    chats.put(chat.id, chat);

                    TdApi.ChatPosition[] positions = chat.positions;
                    chat.positions = new TdApi.ChatPosition[0];
                    setChatPositions(chat, positions);
                }
                break;
            }
            case TdApi.UpdateChatTitle.CONSTRUCTOR: {
                TdApi.UpdateChatTitle updateChat = (TdApi.UpdateChatTitle) update;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                if (chat != null) {
                    synchronized (chat) {
                        chat.title = updateChat.title;
                    }
                }
                break;
            }
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                TdApi.UpdateChatLastMessage updateChat = (TdApi.UpdateChatLastMessage) update;
                TdApi.Chat chat = chats.get(updateChat.chatId);
                if (chat != null) {
                    synchronized (chat) {
                        chat.lastMessage = updateChat.lastMessage;
                        setChatPositions(chat, updateChat.positions);
                    }
                }
                break;
            }
            case TdApi.UpdateChatPosition.CONSTRUCTOR: {
                TdApi.UpdateChatPosition updateChat = (TdApi.UpdateChatPosition) update;
                if (updateChat.position.list.getConstructor() != TdApi.ChatListMain.CONSTRUCTOR) {
                    break;
                }

                TdApi.Chat chat = chats.get(updateChat.chatId);
                if (chat != null) {
                    synchronized (chat) {
                        int i;
                        for (i = 0; i < chat.positions.length; i++) {
                            if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                                break;
                            }
                        }
                        TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[chat.positions.length + (updateChat.position.order == 0 ? 0 : 1) - (i < chat.positions.length ? 1 : 0)];
                        int pos = 0;
                        if (updateChat.position.order != 0) {
                            new_positions[pos++] = updateChat.position;
                        }
                        for (int j = 0; j < chat.positions.length; j++) {
                            if (j != i) {
                                new_positions[pos++] = chat.positions[j];
                            }
                        }

                        setChatPositions(chat, new_positions);
                    }
                }
                break;
            }
        }
    }

    private void setChatPositions(TdApi.Chat chat, TdApi.ChatPosition[] positions) {
        synchronized (mainChatList) {
            synchronized (chat) {
                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        mainChatList.remove(new OrderedChat(chat.id, position));
                    }
                }

                chat.positions = positions;

                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        mainChatList.add(new OrderedChat(chat.id, position));
                    }
                }
            }
        }
    }

    private ChatResponseDto convertToDto(TdApi.Chat chat) {
        ChatResponseDto dto = new ChatResponseDto();
        dto.setId(chat.id);
        dto.setTitle(chat.title);
        dto.setType(getChatType(chat.type));
        
        if (chat.lastMessage != null) {
            dto.setLastMessageDate(chat.lastMessage.date);
            dto.setLastMessageText(extractMessageText(chat.lastMessage));
        }
        
        return dto;
    }

    private String getChatType(TdApi.ChatType type) {
        if (type instanceof TdApi.ChatTypePrivate) {
            return "private";
        } else if (type instanceof TdApi.ChatTypeBasicGroup) {
            return "group";
        } else if (type instanceof TdApi.ChatTypeSupergroup) {
            TdApi.ChatTypeSupergroup supergroup = (TdApi.ChatTypeSupergroup) type;
            return supergroup.isChannel ? "channel" : "supergroup";
        } else if (type instanceof TdApi.ChatTypeSecret) {
            return "secret";
        }
        return "unknown";
    }

    private String extractMessageText(TdApi.Message message) {
        if (message.content instanceof TdApi.MessageText textMessage) {
            return textMessage.text.text;
        }
        return ""; // Для других типов сообщений
    }

    // Вспомогательный класс для сортировки чатов как в примере
    private static class OrderedChat implements Comparable<OrderedChat> {
        final long chatId;
        final TdApi.ChatPosition position;

        OrderedChat(long chatId, TdApi.ChatPosition position) {
            this.chatId = chatId;
            this.position = position;
        }

        @Override
        public int compareTo(OrderedChat o) {
            if (this.position.order != o.position.order) {
                return o.position.order < this.position.order ? -1 : 1;
            }
            if (this.chatId != o.chatId) {
                return o.chatId < this.chatId ? -1 : 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OrderedChat)) return false;
            OrderedChat o = (OrderedChat) obj;
            return this.chatId == o.chatId && this.position.order == o.position.order;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(chatId) ^ Long.hashCode(position.order);
        }
    }
} 