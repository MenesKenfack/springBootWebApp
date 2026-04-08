package com.kaksha.library.config;

import sendinblue.ApiClient;
import sendinblue.auth.ApiKeyAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {
    
    @Value("${brevo.api-key:}")
    private String brevoApiKey;
    
    @Bean
    public ApiClient brevoApiClient() {
        ApiClient client = sendinblue.Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) client.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);
        return client;
    }
}
