package com.iron.mymarket.config;

import com.iron.payment.client.api.DefaultApi;
import com.iron.payment.client.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class PaymentClientConfig {

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Bean
    public DefaultApi paymentApi() {
        log.info("Payment service URL configured as: {}", paymentServiceUrl);

        ApiClient apiClient = new ApiClient(WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .build());
        
        DefaultApi defaultApi = new DefaultApi(apiClient);
        log.info("Payment API client created successfully");
        return defaultApi;
    }
}
