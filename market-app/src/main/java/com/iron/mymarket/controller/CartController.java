package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.UserRepository;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.PaymentHealthService;
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
    private final UserRepository userRepository;

    public CartController(CartService cartService, PaymentHealthService paymentHealthService, UserRepository userRepository) {
        this.cartService = cartService;
        this.paymentHealthService = paymentHealthService;
        this.userRepository = userRepository;
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> getItemsInCart(@AuthenticationPrincipal OAuth2User principal) {
        // 1. Если пользователя нет (не залогинен), возвращаем пустые данные или редирект
        if (principal == null) {
            return Mono.zip(paymentHealthService.isPaymentServiceAvailable(), Mono.just(0L), Mono.just(List.of()))
                    .map(tuple -> buildRendering(false, tuple.getT1(), tuple.getT2(), tuple.getT3()));
        }

        String externalId = principal.getAttribute("sub");

        // 2. Если залогинен — идем в БД за нашим внутренним ID
        return userRepository.findByExternalId(externalId)
                .flatMap(user -> Mono.zip(
                        paymentHealthService.isPaymentServiceAvailable(),
                        cartService.getTotal(user.getId()),
                        cartService.getCartItems(user.getId()).collectList()
                ))
                .map(tuple -> buildRendering(true, tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    @PostMapping("/cart/items")
    public Mono<Rendering> changeItemCountOnCartPage(
            @AuthenticationPrincipal OAuth2User principal,
            ServerWebExchange exchange) {

        return exchange.getFormData().flatMap(formData -> {
            // 1. Валидация входных данных
            long itemId;
            ItemAction action;
            try {
                itemId = Long.parseLong(Objects.requireNonNull(formData.getFirst("id")));
                action = ItemAction.valueOf(formData.getFirst("action"));
            } catch (IllegalArgumentException | NullPointerException e) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid params"));
            }

            if (principal == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            }

            String externalId = principal.getAttribute("sub");

            // 2. Основная логика: Найти юзера -> Изменить товар -> Собрать данные для рендеринга
            return userRepository.findByExternalId(externalId)
                    .flatMap(user -> cartService.changeItemCount(itemId, action, user.getId())
                            .then(Mono.zip(
                                    paymentHealthService.isPaymentServiceAvailable(),
                                    cartService.getCartItems(user.getId()).collectList(),
                                    cartService.getTotal(user.getId())
                            ))
                            .map(tuple -> {
                                boolean isPayAvailable = tuple.getT1();
                                List<ItemDto> items = tuple.getT2();
                                Long total = tuple.getT3();

                                return buildRendering(true, isPayAvailable, total, items);
                            })
                    );
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
}
