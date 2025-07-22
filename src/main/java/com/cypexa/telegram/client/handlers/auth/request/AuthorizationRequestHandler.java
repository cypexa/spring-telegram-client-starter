package com.cypexa.telegram.client.handlers.auth.request;

import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

@Slf4j
public class AuthorizationRequestHandler implements Client.ResultHandler {
    @Override
    public void onResult(TdApi.Object object) {
        if (object instanceof TdApi.Error error) {
            log.error("Authorization request failed: {}", error.message);
        }
    }
}
