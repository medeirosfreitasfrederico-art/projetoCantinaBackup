package com.ifsp.projetoCantina.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfig {
    
    @Value("${mercadopago.access-token}")
    private String accessToken;
    
    @Value("${mercadopago.base-url}")
    private String baseUrl;
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
}