package com.cypexa.telegram.client.service;

import com.cypexa.telegram.client.handlers.auth.request.AuthorizationRequestHandler;
import com.cypexa.telegram.client.handlers.message.LogMessageHandler;
import com.cypexa.telegram.client.properties.TelegramClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class TelegramAuthService {

    private final TelegramClientProperties properties;
    private final Lock authorizationLock = new ReentrantLock();
    private final Condition gotAuthorization = authorizationLock.newCondition();
    private Client client;
    private volatile TdApi.AuthorizationState authorizationState;
    private volatile boolean haveAuthorization = false;

    @Autowired
    public TelegramAuthService(TelegramClientProperties properties) {
        this.properties = properties;
        initializeClient();
    }

    private void initializeClient() {
        Client.setLogMessageHandler(properties.getLogVerbosityLevel(), new LogMessageHandler());
        try {
            Client.execute(new TdApi.SetLogVerbosityLevel(0));
            Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false)));
        } catch (Client.ExecutionException error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }
        client = Client.create(this::onUpdate, null, null);
    }

    private void onUpdate(TdApi.Object update) {
        if (update instanceof TdApi.UpdateAuthorizationState) {
            onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) update).authorizationState);
        }
    }

    private void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
        if (authorizationState != null) {
            this.authorizationState = authorizationState;
        }

        log.info("Authorization state updated: {}", this.authorizationState.getClass().getSimpleName());

        switch (this.authorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                setTdlibParameters();
                break;
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
                haveAuthorization = true;
                authorizationLock.lock();
                try {
                    gotAuthorization.signal();
                } finally {
                    authorizationLock.unlock();
                }
                break;
            case TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR:
                haveAuthorization = false;
                log.info("Logging out");
                break;
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR:
                haveAuthorization = false;
                log.info("Closing");
                break;
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR:
                log.info("Closed");
                break;
        }
    }

    private void setTdlibParameters() {
        TdApi.SetTdlibParameters request = new TdApi.SetTdlibParameters();
        request.databaseDirectory = properties.getDatabaseDirectory();
        request.useMessageDatabase = true;
        request.useSecretChats = true;
        request.apiId = properties.getApiId();
        request.apiHash = properties.getApiHash();
        request.systemLanguageCode = properties.getSystemLanguageCode();
        request.deviceModel = properties.getDeviceModel();
        request.applicationVersion = properties.getApplicationVersion();
        request.useTestDc = properties.getUseTestDc();
        request.filesDirectory = properties.getFilesDirectory();

        client.send(request, new AuthorizationRequestHandler());
    }

    public CompletableFuture<String> sendPhoneNumber(String phoneNumber) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (authorizationState == null) {
            future.completeExceptionally(new RuntimeException("Client not initialized"));
            return future;
        }

        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitPhoneNumber)) {
            future.completeExceptionally(new RuntimeException("Current state is not waiting for phone number. Current state: " + authorizationState.getClass()
                    .getSimpleName()));
            return future;
        }

        client.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), object -> {
            if (object instanceof TdApi.Ok) {
                future.complete("Phone number sent successfully");
            } else if (object instanceof TdApi.Error error) {
                future.completeExceptionally(new RuntimeException("Error sending phone number: " + error.message));
            } else {
                future.completeExceptionally(new RuntimeException("Unexpected response type: " + object.getClass()
                        .getSimpleName()));
            }
        });

        return future;
    }

    public CompletableFuture<String> sendAuthCode(String code) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (authorizationState == null) {
            future.completeExceptionally(new RuntimeException("Client not initialized"));
            return future;
        }

        if (!(authorizationState instanceof TdApi.AuthorizationStateWaitCode)) {
            future.completeExceptionally(new RuntimeException("Current state is not waiting for code. Current state: " + authorizationState.getClass()
                    .getSimpleName()));
            return future;
        }

        client.send(new TdApi.CheckAuthenticationCode(code), new Client.ResultHandler() {
            @Override
            public void onResult(TdApi.Object object) {
                if (object instanceof TdApi.Ok) {
                    future.complete("Authentication code verified successfully");
                } else if (object instanceof TdApi.Error error) {
                    future.completeExceptionally(new RuntimeException("Error verifying code: " + error.message));
                } else {
                    future.completeExceptionally(new RuntimeException("Unexpected response type: " + object.getClass()
                            .getSimpleName()));
                }
            }
        });

        return future;
    }

    public String getCurrentAuthState() {
        if (authorizationState == null) {
            return "NOT_INITIALIZED";
        }

        return authorizationState.getClass().getSimpleName();
    }

    public boolean isAuthorized() {
        return haveAuthorization;
    }

} 