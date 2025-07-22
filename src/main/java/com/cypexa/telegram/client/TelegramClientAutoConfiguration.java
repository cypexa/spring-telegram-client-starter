package com.cypexa.telegram.client;

import com.cypexa.telegram.client.controller.TelegramAuthController;
import com.cypexa.telegram.client.service.TelegramAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "spring.telegram.client",
        name = {
                "api-id",
                "api-hash"
        }
)
@ConfigurationPropertiesScan(basePackages = "com.cypexa.telegram.client.properties")
@ComponentScan(basePackages = {
        "com.cypexa.telegram.client.service",
        "com.cypexa.telegram.client.controller"
})
@Slf4j
public class TelegramClientAutoConfiguration {
    
    @Bean
    public TelegramAuthService telegramAuthService(
            com.cypexa.telegram.client.properties.TelegramClientProperties properties) {
        log.info("Initializing TelegramAuthService with properties: apiId={}, databaseDirectory={}", 
                properties.getApiId(), properties.getDatabaseDirectory());
        return new TelegramAuthService(properties);
    }
    
    @Bean
    public TelegramAuthController telegramAuthController(TelegramAuthService authService) {
        log.info("Initializing TelegramAuthController");
        return new TelegramAuthController(authService);
    }
}
