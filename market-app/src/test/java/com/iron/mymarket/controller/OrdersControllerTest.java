package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.model.OrderItemDto;
import com.iron.mymarket.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.*;

@WebFluxTest(OrdersController.class)
public class OrdersControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartStorage cartStorage;

    private OrderDto order1;
    private OrderDto order2;

    @BeforeEach
    void setUp() {
        order1 = new OrderDto(1L, List.of(), 1000);
        order2 = new OrderDto(2L, List.of(), 2500);
    }

    @Test
    void getOrders_shouldReturnOrdersPageWithOrders() {
        when(orderService.findOrders()).thenReturn(Flux.just(order1, order2));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("1000");
                    assert html.contains("2500");
                });

        verify(orderService, times(1)).findOrders();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createNewOrder_shouldRedirectToNewOrderPage() {
        OrderDto newOrder = new OrderDto(99L, List.of(), 5000);
        when(orderService.createNewOrder(any(CartStorage.class))).thenReturn(Mono.just(newOrder));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/99?newOrder=true");

        verify(orderService, times(1)).createNewOrder(any(CartStorage.class));
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrderById_shouldReturnValidOrder() {

        List<OrderItemDto> orderItemsDto = List.of(
                new OrderItemDto(new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                        1, 100),
                new OrderItemDto(new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 200, 0),
                        1, 200)
        );

        OrderDto order = new OrderDto(1L, orderItemsDto, 300);

        when(orderService.findOrderById(1L)).thenReturn(Mono.just(order));

        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Item 1");
                    assert html.contains("Item 2");
                });
    }
}
