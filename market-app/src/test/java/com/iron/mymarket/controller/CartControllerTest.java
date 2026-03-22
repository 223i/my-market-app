package com.iron.mymarket.controller;

import com.iron.mymarket.configuration.CustomAuthenticationEntryPoint;
import com.iron.mymarket.configuration.SecurityConfig;
import com.iron.mymarket.configuration.TestConfig;
import com.iron.mymarket.dao.entities.User;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.CustomOidcUserService;
import com.iron.mymarket.service.PaymentHealthService;
import com.iron.mymarket.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(CartController.class)
@Import({SecurityConfig.class, TestConfig.class})
public class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private PaymentHealthService paymentHealthService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private CustomOidcUserService oidcUserService;

    private OAuth2User mockOAuth2User;
    private OAuth2User mockOAuth2User2;

    @BeforeEach
    void setUp() {
        mockOAuth2User = new OAuth2User() {
            @Override
            public Map<String, Object> getAttributes() {
                return Map.of(
                        "sub", "user123",
                        "name", "Test User",
                        "email", "test@example.com"
                );
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }

            @Override
            public String getName() {
                return "Test User";
            }
        };

        mockOAuth2User2 = new OAuth2User() {
            @Override
            public Map<String, Object> getAttributes() {
                return Map.of(
                        "sub", "user456",
                        "name", "Another User",
                        "email", "another@example.com"
                );
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }

            @Override
            public String getName() {
                return "Another User";
            }
        };
    }

    @Test
    void getCart_withAuthenticatedUser_shouldReturnUserSpecificCart() {
        User mockUser = new User("user123", "test@example.com", "Test User");
        mockUser.setId(1L);
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(mockUser));
        when(cartService.getCartItems(1L)).thenReturn(Flux.just(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 2),
                new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 200, 1)
        ));
        when(cartService.getTotal(1L)).thenReturn(Mono.just(400L));
        when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html")
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1");
                    assert html.contains("Item 2");
                    assert html.contains("400");
                });

        verify(userService, times(1)).findByExternalId("user123");
        verify(cartService, times(1)).getCartItems(1L);
        verify(cartService, times(1)).getTotal(1L);
        verify(paymentHealthService, times(1)).isPaymentServiceAvailable();
    }

    @Test
    void addToCart_withAuthenticatedUser_shouldAddItemToUserCart() {
        User mockUser = new User("user123", "test@example.com", "Test User");
        mockUser.setId(1L);
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(mockUser));
        when(cartService.changeItemCount(1L, ItemAction.PLUS, 1L)).thenReturn(Mono.empty());
        when(cartService.getCartItems(1L)).thenReturn(Flux.just(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 1)
        ));
        when(cartService.getTotal(1L)).thenReturn(Mono.just(100L));
        when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .mutateWith(csrf())
                .post()
                .uri("/cart/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html");

        verify(userService, times(1)).findByExternalId("user123");
        verify(cartService, times(1)).changeItemCount(1L, com.iron.mymarket.model.ItemAction.PLUS, 1L);
    }

    @Test
    void addToCart_withAnonymousUser_shouldRedirectToLogin() {
        when(authenticationEntryPoint.commence(any(), any())).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
            exchange.getResponse().getHeaders().setLocation(java.net.URI.create("/auth/login"));
            return exchange.getResponse().setComplete();
        });

        webTestClient.post()
                .uri("/cart/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/auth/login");

        verifyNoInteractions(cartService);
        verifyNoInteractions(userService);
    }

    @Test
    void removeFromCart_withAuthenticatedUser_shouldRemoveFromUserCart() {
        User mockUser = new User("user123", "test@example.com", "Test User");
        mockUser.setId(1L);
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(mockUser));
        when(cartService.changeItemCount(1L, com.iron.mymarket.model.ItemAction.DELETE, 1L)).thenReturn(Mono.empty());
        when(cartService.getCartItems(1L)).thenReturn(Flux.empty());
        when(cartService.getTotal(1L)).thenReturn(Mono.just(0L));
        when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .mutateWith(csrf())
                .post()
                .uri("/cart/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "DELETE"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html");

        verify(userService, times(1)).findByExternalId("user123");
        verify(cartService, times(1)).changeItemCount(1L, ItemAction.DELETE, 1L);
    }

    @Test
    void cartIsolation_differentUsers_shouldHaveSeparateCarts() {
        // Given - User 1's cart
        User mockUser = new User("user123", "test@example.com", "Test User");
        mockUser.setId(1L);
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(mockUser));
        when(cartService.getCartItems(1L)).thenReturn(Flux.just(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 2)
        ));
        when(cartService.getTotal(1L)).thenReturn(Mono.just(200L));
        when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        // When & Then - User 1 sees their cart
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1");
                    assert html.contains("200");
                });

        // Given - User 2's cart
        User mockUser2 = new User("user456", "another@example.com", "Another User");
        mockUser2.setId(2L);
        when(userService.findByExternalId("user456")).thenReturn(Mono.just(mockUser2));
        when(cartService.getCartItems(2L)).thenReturn(Flux.just(
                new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 300, 1)
        ));
        when(cartService.getTotal(2L)).thenReturn(Mono.just(300L));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User2))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 2");
                    assert html.contains("300");
                });

        verify(cartService, times(1)).getCartItems(1L);
        verify(cartService, times(1)).getCartItems(2L);
        verify(cartService, never()).getCartItems(999L); // No cross-contamination
    }

    @Test
    void getCart_withUserNotFound_shouldRedirectToLoginWithError() {
        when(userService.findByExternalId("user123")).thenReturn(Mono.empty());
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/auth/login?error=user_not_registered");

        verify(userService, times(1)).findByExternalId("user123");
        verifyNoInteractions(cartService);
        verifyNoInteractions(paymentHealthService);
    }

    @Test
    void getItemsInCart_shouldReturnEmptyCart() {
        User mockUser = new User("user123", "test@example.com", "Test User");
        mockUser.setId(1L);
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(mockUser));
        Mockito.when(cartService.getCartItems(any())).thenReturn(Flux.empty());
        Mockito.when(cartService.getTotal(any())).thenReturn(Mono.just(0L));
        Mockito.when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        webTestClient
                // ОБЯЗАТЕЛЬНО: добавляем мок пользователя, иначе Security не пустит к контроллеру
                .mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk() // Теперь вернет 200
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("0"); // Проверяем, что в корзине 0
                });
    }
}
