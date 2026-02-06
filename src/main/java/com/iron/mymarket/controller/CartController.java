package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> getItemsInCart(WebSession session) {
        CartStorage cart = session.getAttribute("cart");

        return Mono.just(Rendering.view("cart")
                .modelAttribute("items", cartService.getCartItems(cart))
                .modelAttribute("total", cartService.getTotal(cart))
                .build());
    }

    @PostMapping("/cart/items")
    public Mono<Rendering> changeItemCountOnCartPage(ServerWebExchange exchange, WebSession session) {
        return exchange.getFormData().flatMap(formData -> {
            Long id = Long.valueOf(Objects.requireNonNull(formData.getFirst("id")));
            ItemAction action = ItemAction.valueOf(formData.getFirst("action"));
            CartStorage cart = session.getAttributeOrDefault("cart", new CartStorage());


            return cartService.changeItemCount(id, action, cart)
                    .flatMap(updatedCart -> {
                        session.getAttributes().put("cart", updatedCart);
                        return session.save();
                    })
                    .then(Mono.just(Rendering.view("cart")
                            .modelAttribute("items", cartService.getCartItems(cart))
                            .modelAttribute("total", cartService.getTotal(cart))
                            .build()));
        });
    }
}
