package com.iron.mymarket.configuration;

import com.iron.mymarket.dao.repository.UserRepository;
import com.iron.mymarket.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static reactor.core.publisher.Flux.empty;

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

    @Bean
    @Primary
    public ItemService itemService() {
        ItemService mock = mock(ItemService.class);
        when(mock.findItems(anyString(), any(), anyInt(), anyInt())).thenReturn(empty());
        return mock;
    }

    @Bean
    @Primary
    public CartService cartService() {
        CartService mock = mock(CartService.class);
        when(mock.getCartItems(anyLong())).thenReturn(empty());
        return mock;
    }

    @Bean
    @Primary
    public OrderService orderService() {
        return mock(OrderService.class);
    }

    @Bean
    @Primary
    public PaymentClientService paymentClientService() {
        return mock(PaymentClientService.class);
    }

    @Bean
    @Primary
    public UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    @Primary
    public CacheService cacheService() {
        return mock(CacheService.class);
    }

    @Bean
    @Primary
    public PaymentHealthService paymentHealthService() {
        return mock(PaymentHealthService.class);
    }
}
