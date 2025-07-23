package com.cypexa.telegram.client;

import com.cypexa.telegram.client.config.TelegramClientConfiguration;
import com.cypexa.telegram.client.properties.TelegramClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(
        prefix = "spring.telegram.client",
        name = {"api-id", "api-hash"}
)
@EnableConfigurationProperties(TelegramClientProperties.class)
@ComponentScan(basePackages = "com.cypexa.telegram.client")
@Import(TelegramClientConfiguration.class)
public class TelegramClientAutoConfiguration {
}
