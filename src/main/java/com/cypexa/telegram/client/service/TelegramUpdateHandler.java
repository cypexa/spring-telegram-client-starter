package com.cypexa.telegram.client.service;

import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.TdApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TelegramUpdateHandler {

    private final ApplicationContext applicationContext;

    @Autowired
    public TelegramUpdateHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void handleUpdate(TdApi.Object update) {
        try {
            if (update instanceof TdApi.UpdateAuthorizationState) {
                // Делегируем обработку аутентификации в TelegramAuthService
                getTelegramAuthService().handleAuthorizationUpdate(
                        ((TdApi.UpdateAuthorizationState) update).authorizationState
                );
            } else {
                // Делегируем обработку чатов в TelegramChatService
                getTelegramChatService().handleUpdate(update);
            }
        } catch (Exception e) {
            log.error("Error handling update: {}", update.getClass().getSimpleName(), e);
        }
    }

    private TelegramAuthService getTelegramAuthService() {
        return applicationContext.getBean(TelegramAuthService.class);
    }

    private TelegramChatService getTelegramChatService() {
        return applicationContext.getBean(TelegramChatService.class);
    }
} 