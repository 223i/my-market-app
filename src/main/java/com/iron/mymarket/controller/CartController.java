package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> getItemsInCart() {
        return Mono.just(Rendering.view("cart")
                .modelAttribute("items", cartService.getCartItems())
                .modelAttribute("total", cartService.getTotal())
                .build());
    }

    @PostMapping("/cart/items")
    public Mono<Rendering> changeItemCountOnCartPage(
            @RequestParam long id,
            @RequestParam ItemAction action) {
        return cartService.changeItemCount(id, action)
                .onErrorComplete()
                .then(Mono.just(Rendering.view("cart")
                        .modelAttribute("items", cartService.getCartItems())
                        .modelAttribute("total", cartService.getTotal())
                        .build()));
    }
}
