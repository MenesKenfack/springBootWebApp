package com.kaksha.library.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class CampayConfig {
    
    @Value("${campay.api-key:}")
    private String apiKey;
    
    @Value("${campay.api-secret:}")
    private String apiSecret;
    
    @Value("${campay.webhook-secret:}")
    private String webhookSecret;
    
    @Value("${campay.base-url:https://demo.campay.net/api}")
    private String baseUrl;
    
    @Value("${campay.currency:XAF}")
    private String currency;
    
    @Value("${campay.callback-url:}")
    private String callbackUrl;
}
