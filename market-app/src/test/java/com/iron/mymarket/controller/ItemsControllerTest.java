package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest(ItemsController.class)
class ItemsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private ItemService itemService;

    @Test
    void getItems_shouldReturnItemsPageWithItemsAndTotal() {
        Flux<ItemDto> items = Flux.just(
                new ItemDto(1L, "Item 1", "Desc1", "/img/1.jpg", 100, 0),
                new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 200, 0)
        );

        when(itemService.findItems(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(items);

        webTestClient.get()
                .uri("/items")
                .exchange().expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1");
                    assert html.contains("Item 2");
                });

        verify(itemService).findItems("", ItemSort.NO, 1, 6);
    }

    @Test
    void getItems_rootPath_shouldWorkSameAsItems() {
        when(itemService.findItems(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Витрина магазина");
                    assert html.contains("Заказы");
                    assert html.contains("Корзина");
                });
    }

    @Test
    void getItems_withSearchAndSort_shouldPassParamsToService() {
        when(itemService.findItems("phone", ItemSort.PRICE, 2, 11))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("search", "phone")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageNumber", 2)
                        .queryParam("pageSize", 10)
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(itemService).findItems("phone", ItemSort.PRICE, 2, 11);
    }

    @Test
    void getItemById_shouldReturnItemCorrectly() {
        ItemDto item = new ItemDto(1L, "1", "ItemByd1", "", 100, 0);

        when(itemService.getItemById(1L)).thenReturn(Mono.just(item));

        webTestClient.get()
                .uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("ItemByd1");
                });

        verify(itemService, times(1)).getItemById(1L);
        verifyNoMoreInteractions(itemService);
    }
}
