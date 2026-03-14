package com.iron.mymarket.controller;

import com.iron.mymarket.dao.session.CartStorage;
import com.iron.mymarket.service.OrderService;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.PaymentHealthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

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
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated())
                .defaultIfEmpty(false)
                .map(isAuthenticated -> Rendering.view("orders")
                        .modelAttribute("orders", orderService.findOrders())
                        .modelAttribute("isAuthenticated", isAuthenticated)
                        .build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrderById(@PathVariable Long id,
                                        @RequestParam(required = false,
                                                value = "newOrder", defaultValue = "false") Boolean newOrder) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated())
                .defaultIfEmpty(false)
                .zipWith(orderService.findOrderById(id))
                .map(tuple -> Rendering.view("order")
                        .modelAttribute("order", tuple.getT2())
                        .modelAttribute("isAuthenticated", tuple.getT1())
                        .build());
    }

    @PostMapping("/buy")
    public Mono<Rendering> createNewOrder(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Mono.just(Rendering.redirectTo("/auth/login").build());
        }
        Long userId = principal.getAttribute("internal_id");

        // 1. Проверяем, не пуста ли корзина в БД
        return cartService.getCartItems(userId).collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return renderCartWithError(userId, "Ваша корзина пуста", true);
                    }

                    // 2. Проверяем доступность сервиса оплаты
                    return paymentHealthService.isPaymentServiceAvailable()
                            .flatMap(isAvailable -> {
                                if (!isAvailable) {
                                    return renderCartWithError(userId, "Сервис оплаты временно недоступен", false);
                                }

                                // 3. Создаем заказ (orderService теперь тоже должен принимать userId вместо CartStorage)
                                return orderService.createNewOrderWithPayment(userId)
                                        .map(order -> Rendering.redirectTo("/orders/" + order.getId() + "?newOrder=true").build())
                                        .onErrorResume(e -> renderCartWithError(userId, e.getMessage(), true));
                            });
                });
    }

    // Вспомогательный метод для рендеринга ошибок, чтобы не дублировать код
    private Mono<Rendering> renderCartWithError(Long userId, String error, boolean isPayAvailable) {
        return Mono.zip(
                cartService.getCartItems(userId).collectList(),
                cartService.getTotal(userId)
        ).map(tuple -> Rendering.view("cart")
                .modelAttribute("error", error)
                .modelAttribute("items", tuple.getT1())
                .modelAttribute("total", tuple.getT2())
                .modelAttribute("paymentServiceAvailable", isPayAvailable)
                .modelAttribute("isAuthenticated", true)
                .build());
    }
}
