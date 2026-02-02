package com.iron.mymarket.controller;

import com.iron.mymarket.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class OrdersController {

    private final OrderService orderService;

    public OrdersController(OrderService orderService) {
        this.orderService = orderService;
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
                .map(order -> Rendering.view("orders")
                        .modelAttribute("order", order)
                        .build());
    }

    @PostMapping("/buy")
    public Mono<String> createNewOrder() {
        return orderService.createNewOrder()
                .map(createdOrder -> "redirect:/orders/%d?newOrder=true" + createdOrder.getId());
    }
}
