package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.service.OrderService;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.PaymentHealthService;
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
    private final PaymentHealthService paymentHealthService;

    public OrdersController(OrderService orderService, CartService cartService, PaymentHealthService paymentHealthService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.paymentHealthService = paymentHealthService;
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
                    .zipWith(paymentHealthService.isPaymentServiceAvailable())
                    .map(tuple -> Rendering.view("cart")
                            .modelAttribute("error", "Cart is empty")
                            .modelAttribute("items", tuple.getT1())
                            .modelAttribute("total", 0L)
                            .modelAttribute("paymentServiceAvailable", tuple.getT2())
                            .modelAttribute("paymentServiceMessage", 
                                    tuple.getT2() ? null : "Сервис оплаты временно недоступен. Попробуйте позже.")
                            .build());
        }

        return paymentHealthService.isPaymentServiceAvailable()
                .flatMap(isAvailable -> {
                    if (!isAvailable) {
                        return cartService.getCartItems(cart)
                                .collectList()
                                .zipWith(cartService.getTotal(cart))
                                .map(tuple -> Rendering.view("cart")
                                        .modelAttribute("error", "Сервис оплаты временно недоступен. Попробуйте позже.")
                                        .modelAttribute("items", tuple.getT1())
                                        .modelAttribute("total", tuple.getT2())
                                        .modelAttribute("paymentServiceAvailable", false)
                                        .modelAttribute("paymentServiceMessage", "Сервис оплаты временно недоступен. Попробуйте позже.")
                                        .build());
                    }

                    return orderService.createNewOrderWithPayment(cart)
                            .flatMap(createdOrder -> session.save()
                                    .thenReturn(Rendering.redirectTo("/orders/" + createdOrder.getId() + "?newOrder=true").build()))
                            .onErrorResume(e -> cartService.getCartItems(cart)
                                    .collectList()
                                    .zipWith(cartService.getTotal(cart))
                                    .zipWith(paymentHealthService.isPaymentServiceAvailable())
                                    .map(tuple -> Rendering.view("cart")
                                            .modelAttribute("error", e.getMessage())
                                            .modelAttribute("items", tuple.getT1().getT1())
                                            .modelAttribute("total", tuple.getT1().getT2())
                                            .modelAttribute("paymentServiceAvailable", tuple.getT2())
                                            .modelAttribute("paymentServiceMessage", 
                                                    tuple.getT2() ? null : "Сервис оплаты временно недоступен. Попробуйте позже.")
                                            .build()));
                });
    }
}
