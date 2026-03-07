package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.PaymentHealthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Controller
public class CartController {

    private final CartService cartService;
    private final PaymentHealthService paymentHealthService;

    public CartController(CartService cartService, PaymentHealthService paymentHealthService) {
        this.cartService = cartService;
        this.paymentHealthService = paymentHealthService;
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> getItemsInCart(WebSession session) {
        CartStorage cart = session.getAttribute("cart");

        return paymentHealthService.isPaymentServiceAvailable()
                .zipWith(cartService.getTotal(cart != null ? cart : new CartStorage()))
                .zipWith(cartService.getCartItems(cart != null ? cart : new CartStorage()).collectList())
                .map(tuple -> {
                    Boolean isPaymentAvailable = tuple.getT1().getT1();
                    Long total = tuple.getT1().getT2();
                    List items = tuple.getT2();
                    
                    return Rendering.view("cart")
                            .modelAttribute("items", items)
                            .modelAttribute("total", total)
                            .modelAttribute("paymentServiceAvailable", isPaymentAvailable)
                            .modelAttribute("paymentServiceMessage", 
                                    isPaymentAvailable ? null : "Сервис оплаты временно недоступен. Попробуйте позже.")
                            .build();
                });
    }

    @PostMapping("/cart/items")
    public Mono<Rendering> changeItemCountOnCartPage(ServerWebExchange exchange, WebSession session) {
        return exchange.getFormData().flatMap(formData -> {
            long id;
            ItemAction action;
            try {
                id = Long.parseLong(Objects.requireNonNull(formData.getFirst("id")));
                action = ItemAction.valueOf(formData.getFirst("action"));
            } catch (IllegalArgumentException | NullPointerException e) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid params"));
            }
            CartStorage cart = session.getAttributeOrDefault("cart", new CartStorage());


            return cartService.changeItemCount(id, action, cart)
                    .flatMap(updatedCart -> {
                        session.getAttributes().put("cart", updatedCart);
                        return session.save();
                    })
                    .then(paymentHealthService.isPaymentServiceAvailable()
                    .zipWith(cartService.getTotal(cart))
                    .zipWith(cartService.getCartItems(cart).collectList())
                    .map(tuple -> {
                        Boolean isPaymentAvailable = tuple.getT1().getT1();
                        Long total = tuple.getT1().getT2();
                        List items = tuple.getT2();
                        
                        return Rendering.view("cart")
                                .modelAttribute("items", items)
                                .modelAttribute("total", total)
                                .modelAttribute("paymentServiceAvailable", isPaymentAvailable)
                                .modelAttribute("paymentServiceMessage", 
                                        isPaymentAvailable ? null : "Сервис оплаты временно недоступен. Попробуйте позже.")
                                .build();
                    }));
        });
    }
}
