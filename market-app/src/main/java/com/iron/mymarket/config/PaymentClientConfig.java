package com.iron.mymarket.config;

import com.iron.payment.client.api.DefaultApi;
import com.iron.payment.client.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentClientConfig {

    @Bean
    public DefaultApi paymentApi() {
        ApiClient apiClient = new ApiClient(WebClient.builder()
                .baseUrl("http://localhost:8081")
                .build());
        
        return new DefaultApi(apiClient);
    }
}
