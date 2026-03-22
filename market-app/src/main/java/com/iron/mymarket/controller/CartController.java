package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.PaymentHealthService;
import com.iron.mymarket.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Controller
public class CartController {

    private final CartService cartService;
    private final PaymentHealthService paymentHealthService;
    private final UserService userService;

    public CartController(CartService cartService, PaymentHealthService paymentHealthService, UserService userService) {
        this.cartService = cartService;
        this.paymentHealthService = paymentHealthService;
        this.userService = userService;
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> getItemsInCart(@AuthenticationPrincipal OAuth2User principal) {
        // 1. Если пользователя нет (не залогинен), возвращаем редирект
        if (principal == null) {
            return redirectToLoginWithError();
        }

        String externalId = principal.getAttribute("sub");

        // 2. Если залогинен — идем в БД за нашим внутренним ID
        return userService.findByExternalId(externalId)
                .flatMap(user -> Mono.zip(
                        paymentHealthService.isPaymentServiceAvailable(),
                        cartService.getTotal(user.getId()),
                        cartService.getCartItems(user.getId()).collectList()
                ))
                .map(tuple -> buildRendering(true, tuple.getT1(), tuple.getT2(), tuple.getT3()))
                .switchIfEmpty(redirectToLoginWithError());
    }

    @PostMapping("/cart/items")
    public Mono<Rendering> changeItemCountOnCartPage(
            @AuthenticationPrincipal OAuth2User principal,
            ServerWebExchange exchange) {

        if (principal == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }

        return exchange.getFormData().flatMap(formData -> {
            try {
                long itemId = Long.parseLong(Objects.requireNonNull(formData.getFirst("id")));
                ItemAction action = ItemAction.valueOf(formData.getFirst("action"));

                return userService.findByExternalId(principal.getAttribute("sub"))
                        .flatMap(user -> cartService.changeItemCount(itemId, action, user.getId())
                                .then(Mono.zip(
                                        paymentHealthService.isPaymentServiceAvailable(),
                                        cartService.getTotal(user.getId()),
                                        cartService.getCartItems(user.getId()).collectList()
                                ))
                                .map(t -> buildRendering(true, t.getT1(), t.getT2(), t.getT3())))
                        .switchIfEmpty(redirectToLoginWithError());

            } catch (IllegalArgumentException | NullPointerException e) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid params"));
            }
        });
    }

    private Rendering buildRendering(boolean isAuth, boolean isPayAvailable, Long total, List<?> items) {
        return Rendering.view("cart")
                .modelAttribute("items", items)
                .modelAttribute("total", total)
                .modelAttribute("paymentServiceAvailable", isPayAvailable)
                .modelAttribute("isAuthenticated", isAuth)
                .modelAttribute("paymentServiceMessage",
                        isPayAvailable ? null : "Сервис оплаты временно недоступен.")
                .build();
    }

    private Mono<Rendering> redirectToLoginWithError() {
        return Mono.just(Rendering.redirectTo("/auth/login?error=user_not_registered").build());
    }
}
