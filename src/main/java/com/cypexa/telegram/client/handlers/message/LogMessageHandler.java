package com.cypexa.telegram.client.handlers.message;

import org.drinkless.tdlib.Client;

import java.util.concurrent.atomic.AtomicLong;

public class LogMessageHandler implements Client.LogMessageHandler {
    @Override
    public void onLogMessage(int verbosityLevel, String message) {
        if (verbosityLevel == 0) {
            onFatalError(message);
            return;
        }
        System.err.println(message);
    }

    private void onFatalError(String errorMessage) {
        final class ThrowError implements Runnable {
            private final String errorMessage;
            private final AtomicLong errorThrowTime;

            private ThrowError(String errorMessage, AtomicLong errorThrowTime) {
                this.errorMessage = errorMessage;
                this.errorThrowTime = errorThrowTime;
            }

            @Override
            public void run() {
                if (isDatabaseBrokenError(errorMessage) || isDiskFullError(errorMessage) || isDiskError(errorMessage)) {
                    processExternalError();
                    return;
                }

                errorThrowTime.set(System.currentTimeMillis());
                throw new ClientError("TDLib fatal error: " + errorMessage);
            }

            private void processExternalError() {
                errorThrowTime.set(System.currentTimeMillis());
                throw new ExternalClientError("Fatal error: " + errorMessage);
            }

            final class ClientError extends Error {
                private ClientError(String message) {
                    super(message);
                }
            }

            final class ExternalClientError extends Error {
                public ExternalClientError(String message) {
                    super(message);
                }
            }

            private boolean isDatabaseBrokenError(String message) {
                return message.contains("Wrong key or database is corrupted") ||
                        message.contains("SQL logic error or missing database") ||
                        message.contains("database disk image is malformed") ||
                        message.contains("file is encrypted or is not a database") ||
                        message.contains("unsupported file format") ||
                        message.contains("Database was corrupted and deleted during execution and can't be recreated");
            }

            private boolean isDiskFullError(String message) {
                return message.contains("PosixError : No space left on device") ||
                        message.contains("database or disk is full");
            }

            private boolean isDiskError(String message) {
                return message.contains("I/O error") || message.contains("Structure needs cleaning");
            }
        }

        final AtomicLong errorThrowTime = new AtomicLong(Long.MAX_VALUE);
        new Thread(new ThrowError(errorMessage, errorThrowTime), "TDLib fatal error thread").start();

        // wait at least 10 seconds after the error is thrown
        while (errorThrowTime.get() >= System.currentTimeMillis() - 10000) {
            try {
                Thread.sleep(1000 /* milliseconds */);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
