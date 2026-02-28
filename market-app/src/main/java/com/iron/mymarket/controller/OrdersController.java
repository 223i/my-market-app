package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.service.OrderService;
import com.iron.mymarket.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Controller
public class OrdersController {

    private final OrderService orderService;
    private final CartService cartService;

    public OrdersController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @GetMapping("/orders")
    public Mono<Rendering> getOrders() {
        return Mono.just(Rendering.view("orders")
                .modelAttribute("orders", orderService.findOrders())
                .build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrderById(@PathVariable Long id,
                                        @RequestParam(required = false,
                                                value = "newOrder", defaultValue = "false") Boolean newOrder) {
        return orderService.findOrderById(id)
                .map(order -> Rendering.view("order")
                        .modelAttribute("order", order)
                        .build());
    }

    @PostMapping("/buy")
    public Mono<Rendering> createNewOrder(WebSession session) {

        CartStorage cart = session.getAttribute("cart");
        if (cart == null || cart.getItems().isEmpty()) {
            return cartService.getCartItems(cart != null ? cart : new CartStorage())
                    .collectList()
                    .map(items -> Rendering.view("cart")
                            .modelAttribute("error", "Cart is empty")
                            .modelAttribute("items", items)
                            .modelAttribute("total", 0L)
                            .build());
        }

        return orderService.createNewOrderWithPayment(cart)
                .flatMap(createdOrder -> session.save()
                        .thenReturn(Rendering.redirectTo("/orders/" + createdOrder.getId() + "?newOrder=true").build()))
                .onErrorResume(e -> cartService.getCartItems(cart != null ? cart : new CartStorage())
                        .collectList()
                        .zipWith(cartService.getTotal(cart != null ? cart : new CartStorage()))
                        .map(tuple -> Rendering.view("cart")
                                .modelAttribute("error", e.getMessage())
                                .modelAttribute("items", tuple.getT1())
                                .modelAttribute("total", tuple.getT2())
                                .build()));
    }
}
