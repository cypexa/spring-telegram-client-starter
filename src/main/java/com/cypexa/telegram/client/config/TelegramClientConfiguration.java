package com.cypexa.telegram.client.config;

import com.cypexa.telegram.client.handlers.message.LogMessageHandler;
import com.cypexa.telegram.client.properties.TelegramClientProperties;
import com.cypexa.telegram.client.service.TelegramUpdateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOError;
import java.io.IOException;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TelegramClientConfiguration {

    private final TelegramClientProperties properties;

    @Bean
    public Client telegramClient(@Lazy TelegramUpdateHandler updateHandler) {
        log.info("Initializing Telegram client");

        // Настройка логирования
        Client.setLogMessageHandler(properties.getLogVerbosityLevel(), new LogMessageHandler());

        try {
            Client.execute(new TdApi.SetLogVerbosityLevel(0));
            Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false)));
        } catch (Client.ExecutionException error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        // Создаем клиент с переданным обработчиком обновлений
        return Client.create(updateHandler::handleUpdate, null, null);
    }
} 