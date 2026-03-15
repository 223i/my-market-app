package com.iron.mymarket.configuration;

import com.iron.mymarket.dao.repository.UserRepository;
import com.iron.mymarket.service.CustomOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

import static org.mockito.Mockito.mock;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    public CustomOidcUserService customOidcUserService(UserRepository userRepository) {
        return mock(CustomOidcUserService.class);
    }

    @Bean
    @Primary
    public UserRepository userRepository() {
        return mock(UserRepository.class);
    }

    @Bean
    @Primary
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return mock(CustomAuthenticationEntryPoint.class);
    }

    @Bean
    @Primary
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository() {
        return mock(ReactiveClientRegistrationRepository.class);
    }

    @Bean
    @Primary
    public ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository() {
        return mock(ServerOAuth2AuthorizedClientRepository.class);
    }
}
