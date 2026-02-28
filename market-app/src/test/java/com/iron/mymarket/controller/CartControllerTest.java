package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.PaymentHealthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;

@WebFluxTest(CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private PaymentHealthService paymentHealthService;

    @Test
    void getItemsInCart_shouldReturnCartPageWithItemsAndTotal() {
        Flux<ItemDto> items = Flux.just(
                new ItemDto(1L, "Item 1", "Desc", "img/1.jpg", 100L, 2),
                new ItemDto(2L, "Item 2", "Desc", "img/2.jpg", 200L, 1)
        );

        Mockito.when(cartService.getCartItems(any())).thenReturn(items);
        Mockito.when(cartService.getTotal(any())).thenReturn(Mono.just(400L));
        Mockito.when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1") && html.contains("Item 2");
                    assert html.contains("Итого: 400 руб.");
                });
    }

    @Test
    void changeItemCountOnCartPage_shouldUpdateCartAndReturnCartPage() {
        CartStorage cartStorage = new CartStorage();
        cartStorage.plus(1L);
        List<ItemDto> updatedItems = List.of(
                new ItemDto(1L, "Item 1", "Desc", "img/1.jpg", 100L, 3)
        );

        Mockito.when(cartService.getCartItems(any())).thenReturn(Flux.fromIterable(updatedItems));
        Mockito.when(cartService.getTotal(any())).thenReturn(Mono.just(300L));
        Mockito.when(cartService.changeItemCount(anyLong(), any(), any()))
                .thenReturn(Mono.just(cartStorage));
        Mockito.when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        // when / then
        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1");
                    assert html.contains("Итого: 300 руб.");
                });

        Mockito.verify(cartService).changeItemCount(eq(1L), eq(ItemAction.PLUS), any(CartStorage.class));
    }

    @Test
    void changeItemCountOnCartPage_shouldDeleteItemFromCart() {
        // given
        Mockito.when(cartService.getCartItems(any())).thenReturn(Flux.fromIterable(
                List.of(new ItemDto(1L, "Item 1", "Desc", "img/1.jpg", 100L, 3))));
        Mockito.when(cartService.getTotal(any())).thenReturn(Mono.just(0L));
        Mockito.when(cartService.changeItemCount(anyLong(), any(), any()))
                .thenReturn(Mono.empty());
        Mockito.when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        // when / then
        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "DELETE"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class);

        Mockito.verify(cartService).changeItemCount(eq(1L), eq(ItemAction.DELETE), any(CartStorage.class));
    }

    @Test
    void changeItemCountOnCartPage_shouldReturnBadRequestForInvalidAction() {
        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "INVALID_ACTION"))
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verify(cartService, Mockito.never())
                .changeItemCount(Mockito.anyLong(), Mockito.any(), any(CartStorage.class));
    }

    @Test
    void changeItemCountOnCartPage_shouldCallServiceMethodsInCorrectOrder() {

        CartStorage cartStorage = new CartStorage();
        cartStorage.plus(1L);
        List<ItemDto> updatedItems = List.of(
                new ItemDto(1L, "Item 1", "Desc", "img/1.jpg", 100L, 3)
        );

        Mockito.when(cartService.getCartItems(any())).thenReturn(Flux.fromIterable(updatedItems));
        Mockito.when(cartService.getTotal(any())).thenReturn(Mono.just(0L));
        Mockito.when(cartService.changeItemCount(anyLong(), any(), any()))
                .thenReturn(Mono.empty());
        Mockito.when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "MINUS"))
                .exchange()
                .expectStatus().isOk();

        var inOrder = Mockito.inOrder(cartService);

        inOrder.verify(cartService).changeItemCount(eq(1L), eq(ItemAction.MINUS), any(CartStorage.class));
        inOrder.verify(cartService).getCartItems(any(CartStorage.class));
        inOrder.verify(cartService).getTotal(any());
    }

    @Test
    void getItemsInCart_shouldReturnEmptyCart() {
        // given
        Mockito.when(cartService.getCartItems(any())).thenReturn(Flux.empty());
        Mockito.when(cartService.getTotal(any())).thenReturn(Mono.just(0L));
        Mockito.when(paymentHealthService.isPaymentServiceAvailable()).thenReturn(Mono.just(true));

        // when / then
        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }
}
