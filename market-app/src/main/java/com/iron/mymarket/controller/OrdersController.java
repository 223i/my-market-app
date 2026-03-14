package com.iron.mymarket.controller;

import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.OrderService;
import com.iron.mymarket.service.PaymentHealthService;
import com.iron.mymarket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
public class OrdersController {

    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentHealthService paymentHealthService;
    private final UserService userService;

    public OrdersController(OrderService orderService, CartService cartService, PaymentHealthService paymentHealthService, UserService userService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.paymentHealthService = paymentHealthService;
        this.userService = userService;
    }

    @GetMapping("/orders")
    public Mono<Rendering> getOrders(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Mono.just(Rendering.redirectTo("/auth/login").build());
        }

        Long userId = principal.getAttribute("internal_id");

        return orderService.findAllOrdersByUserId(userId)
                .collectList()
                .map(orders -> Rendering.view("orders")
                        .modelAttribute("orders", orders)
                        .modelAttribute("isAuthenticated", true)
                        .build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrderById(@PathVariable Long id,
                                        @RequestParam(required = false,
                                                value = "newOrder", defaultValue = "false") Boolean newOrder,
                                        @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Mono.just(Rendering.redirectTo("/auth/login").build());
        }
        Long userId = principal.getAttribute("internal_id");

        return orderService.findOrderByIdAndUserId(id, userId)
                .map(orderDto -> Rendering.view("order")
                        .modelAttribute("order", orderDto)
                        .modelAttribute("isAuthenticated", true)
                        .build())
                .switchIfEmpty(Mono.just(Rendering.redirectTo("/orders?error=not_found").build()));
    }

    @PostMapping("/buy")
    public Mono<Rendering> createNewOrder(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Mono.just(Rendering.redirectTo("/auth/login").build());
        }

        String externalId = principal.getAttribute("sub");

        return userService.findByExternalId(externalId)
                .flatMap(user -> {
                    Long userId = user.getId();

                    return cartService.getCartItems(userId).collectList()
                            .flatMap(items -> {
                                if (items.isEmpty()) {
                                    return renderCartWithError(userId, "Ваша корзина пуста", true);
                                }

                                return paymentHealthService.isPaymentServiceAvailable()
                                        .flatMap(isAvailable -> {
                                            if (!isAvailable) {
                                                return renderCartWithError(userId, "Сервис оплаты недоступен", false);
                                            }

                                            return orderService.createNewOrderWithPayment(userId)
                                                    .map(order -> Rendering.redirectTo("/orders/" + order.getId() + "?newOrder=true").build());
                                        });
                            });
                })
                .onErrorResume(e -> {
                    // Если юзер не найден или произошла ошибка в цепочке
                    log.error("Order creation failed", e);
                    return Mono.just(Rendering.redirectTo("/cart?error=true").build());
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
