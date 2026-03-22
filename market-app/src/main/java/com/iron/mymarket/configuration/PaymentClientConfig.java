package com.iron.mymarket.configuration;

import com.iron.payment.client.api.DefaultApi;
import com.iron.payment.client.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class PaymentClientConfig {

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Bean
    public DefaultApi paymentApi(ReactiveClientRegistrationRepository clientRegistrations,
                                 ServerOAuth2AuthorizedClientRepository authorizedClients) {
        log.info("Configuring Payment API with OAuth2 at: {}", paymentServiceUrl);

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);
        oauth.setDefaultClientRegistrationId("keycloak");


        ApiClient apiClient = new ApiClient(WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .filter(oauth)
                .build());
        
        DefaultApi defaultApi = new DefaultApi(apiClient);
        log.info("Payment API client created successfully");
        return defaultApi;
    }
}
