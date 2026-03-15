package com.iron.mymarket.controller;

import com.iron.mymarket.configuration.CustomAuthenticationEntryPoint;
import com.iron.mymarket.configuration.SecurityConfig;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.model.OrderItemDto;
import com.iron.mymarket.service.*;
import com.iron.mymarket.configuration.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@WebFluxTest(OrdersController.class)
@Import({SecurityConfig.class, TestConfig.class})
public class OrdersControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

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

    private OrderDto order1;
    private OrderDto order2;
    private OAuth2User mockOAuth2User;

    @BeforeEach
    void setUp() {
        // Setup test data
        List<OrderItemDto> orderItems1 = List.of(
                new OrderItemDto(new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0), 1, 100),
                new OrderItemDto(new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 200, 0), 2, 400)
        );
        order1 = new OrderDto(1L, 1L, orderItems1, 500);
        
        List<OrderItemDto> orderItems2 = List.of(
                new OrderItemDto(new ItemDto(3L, "Item 3", "Desc", "/img/3.jpg", 300, 0), 1, 300)
        );
        order2 = new OrderDto(2L, 1L, orderItems2, 300);

        // Setup mock OAuth2 user
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
    }

    @Test
    void getOrders_withAuthenticatedUser_shouldReturnOrdersPageWithUserOrders() {
        // Given
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(new com.iron.mymarket.dao.entities.User("user123", "test@example.com", "Test User")));
        when(orderService.findAllOrdersByUserId(1L)).thenReturn(Flux.just(order1, order2));

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html")
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1");
                    assert html.contains("Item 2");
                    assert html.contains("Item 3");
                    assert html.contains("500"); // total of order1
                    assert html.contains("300"); // total of order2
                });

        verify(userService, times(1)).findByExternalId("user123");
        verify(orderService, times(1)).findAllOrdersByUserId(1L);
    }

    @Test
    void getOrders_withAnonymousUser_shouldRedirectToLogin() {
        // When & Then
        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/auth/login");

        verifyNoInteractions(orderService);
        verifyNoInteractions(userService);
    }

    @Test
    void getOrderById_withAuthenticatedUserAndValidOrder_shouldReturnOrderPage() {
        // Given
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(new com.iron.mymarket.dao.entities.User("user123", "test@example.com", "Test User")));
        when(orderService.findOrderByIdAndUserId(1L, 1L)).thenReturn(Mono.just(order1));

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html")
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1");
                    assert html.contains("Item 2");
                    assert html.contains("500");
                });

        verify(userService, times(1)).findByExternalId("user123");
        verify(orderService, times(1)).findOrderByIdAndUserId(1L, 1L);
    }

    @Test
    void getOrderById_withAuthenticatedUserAndInvalidOrder_shouldRedirectToOrdersWithError() {
        // Given
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(new com.iron.mymarket.dao.entities.User("user123", "test@example.com", "Test User")));
        when(orderService.findOrderByIdAndUserId(999L, 1L)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .get()
                .uri("/orders/999")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders?error=not_found");

        verify(userService, times(1)).findByExternalId("user123");
        verify(orderService, times(1)).findOrderByIdAndUserId(999L, 1L);
    }

    @Test
    void getOrderById_withAnonymousUser_shouldRedirectToLogin() {
        // When & Then
        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/auth/login");

        verifyNoInteractions(orderService);
        verifyNoInteractions(userService);
    }

    @Test
    void createNewOrder_withAuthenticatedUserAndValidCart_shouldCreateOrderAndRedirect() {
        // Given
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(new com.iron.mymarket.dao.entities.User("user123", "test@example.com", "Test User")));
        when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));
        when(cartService.getCartItems(1L)).thenReturn(Flux.just(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 200, 0)
        ));
        when(orderService.createNewOrderWithPayment(1L)).thenReturn(Mono.just(order1));

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/1?newOrder=true");

        verify(userService, times(1)).findByExternalId("user123");
        verify(paymentHealthService, times(1)).isPaymentServiceAvailable();
        verify(cartService, times(1)).getCartItems(1L);
        verify(orderService, times(1)).createNewOrderWithPayment(1L);
    }

    @Test
    void createNewOrder_withAuthenticatedUserAndEmptyCart_shouldReturnCartWithError() {
        // Given
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(new com.iron.mymarket.dao.entities.User("user123", "test@example.com", "Test User")));
        when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));
        when(cartService.getCartItems(1L)).thenReturn(Flux.empty());
        when(cartService.getTotal(1L)).thenReturn(Mono.just(0L));

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html")
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Ваша корзина пуста");
                });

        verify(userService, times(1)).findByExternalId("user123");
        verify(paymentHealthService, times(1)).isPaymentServiceAvailable();
        verify(cartService, times(1)).getCartItems(1L);
        verify(cartService, times(1)).getTotal(1L);
        verifyNoInteractions(orderService);
    }

    @Test
    void createNewOrder_withAuthenticatedUserAndPaymentUnavailable_shouldReturnCartWithError() {
        // Given
        when(userService.findByExternalId("user123")).thenReturn(Mono.just(new com.iron.mymarket.dao.entities.User("user123", "test@example.com", "Test User")));
        when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(false));

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith("text/html")
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Сервис оплаты недоступен");
                });

        verify(userService, times(1)).findByExternalId("user123");
        verify(paymentHealthService, times(1)).isPaymentServiceAvailable();
        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }

    @Test
    void createNewOrder_withAnonymousUser_shouldRedirectToLogin() {
        // When & Then
        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/auth/login");

        verifyNoInteractions(orderService);
        verifyNoInteractions(cartService);
        verifyNoInteractions(paymentHealthService);
        verifyNoInteractions(userService);
    }

    @Test
    void createNewOrder_withUserNotFound_shouldRedirectToLoginWithError() {
        // Given
        when(userService.findByExternalId("user123")).thenReturn(Mono.empty());

        // When & Then
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockOAuth2Login().oauth2User(mockOAuth2User))
                .post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/auth/login?error=user_not_registered");

        verify(userService, times(1)).findByExternalId("user123");
        verifyNoInteractions(paymentHealthService);
        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }
}
