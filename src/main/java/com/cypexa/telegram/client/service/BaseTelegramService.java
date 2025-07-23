package com.cypexa.telegram.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@RequiredArgsConstructor
@Slf4j
public abstract class BaseTelegramService {

    protected final Client telegramClient;
    protected final TelegramAuthService authService;

    /**
     * Выполняет операцию с проверкой авторизации
     */
    protected <T> Mono<T> executeWithAuth(String operationName, AuthorizedOperation<T> operation) {
        return Mono.<T>create(sink -> {
                    if (!authService.isAuthorized()) {
                        sink.error(new RuntimeException("Not authorized"));
                        return;
                    }

                    try {
                        operation.execute(sink);
                    } catch (Exception e) {
                        log.error("Error executing {}: {}", operationName, e.getMessage(), e);
                        sink.error(e);
                    }
                })
                .doOnSubscribe(subscription -> log.debug("Starting operation: {}", operationName))
                .doOnSuccess(result -> log.debug("Successfully completed operation: {}", operationName))
                .doOnError(error -> log.error("Failed operation: {}", operationName, error));
    }

    /**
     * Отправляет запрос через Telegram клиент с обработкой результата
     */
    protected <T> void sendTelegramRequest(TdApi.Function request,
                                           MonoSink<T> sink,
                                           ResultHandler<T> resultHandler) {
        telegramClient.send(request, result -> {
            try {
                if (result instanceof TdApi.Error error) {
                    sink.error(new RuntimeException("Telegram API error: " + error.message));
                } else {
                    resultHandler.handle(result, sink);
                }
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @FunctionalInterface
    protected interface AuthorizedOperation<T> {
        void execute(MonoSink<T> sink) throws Exception;
    }

    @FunctionalInterface
    protected interface ResultHandler<T> {
        void handle(TdApi.Object result, MonoSink<T> sink) throws Exception;
    }
} 