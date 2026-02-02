package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> getItemsInCart(WebSession session) {
        session.getAttributes().putIfAbsent("cart", new CartStorage());
        CartStorage cart = (CartStorage) session.getAttributes().get("cart");
        return Mono.just(Rendering.view("cart")
                .modelAttribute("items", cartService.getCartItems(cart))
                .modelAttribute("total", cartService.getTotal(cart))
                .build());
    }

    @PostMapping("/cart/items")
    public Mono<Rendering> changeItemCountOnCartPage(
            @RequestParam Long id,
            @RequestParam ItemAction action,
            WebSession session) {

        session.getAttributes().putIfAbsent("cart", new CartStorage());
        CartStorage cart = (CartStorage) session.getAttributes().get("cart");
        return cartService.changeItemCount(id, action, cart)
                .onErrorComplete()
                .then(Mono.just(Rendering.view("cart")
                        .modelAttribute("items", cartService.getCartItems(cart))
                        .modelAttribute("total", cartService.getTotal(cart))
                        .build()));
    }
}
