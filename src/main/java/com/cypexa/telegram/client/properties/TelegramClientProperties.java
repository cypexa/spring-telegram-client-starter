package com.cypexa.telegram.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.telegram.client")
public class TelegramClientProperties {
    
    private String databaseEncryptionKey;
    private Integer apiId;
    private String apiHash;
    private String phone;
    private String systemLanguageCode = "en";
    private String deviceModel = "Desktop";
    private String systemVersion = "1.0";
    private String applicationVersion = "1.0";
    private Boolean enableStorageOptimizer = true;
    private Boolean ignoreFileNames = false;
    private Boolean useTestDc = false;
    private String databaseDirectory = "./tdlib";
    private String filesDirectory = "./tdlib";
    private Integer logVerbosityLevel = 0; // 0 - NEVER, 1 - ERROR, 2 - WARNING, 3 - INFO, 4 - DEBUG, 5 - VERBOSE
} 