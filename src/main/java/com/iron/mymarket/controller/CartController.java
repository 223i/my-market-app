package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/items")
    public String getItemsInCart(Model model){
        List<ItemDto> itemsInCart = cartService.getCartItems();
        model.addAttribute("items", itemsInCart);
        model.addAttribute("total", cartService.getTotal());
        return "cart";
    }

    @PostMapping("/cart/items")
    public String changeItemCountOnCartPage(
            @RequestParam long id,
            @RequestParam ItemAction action,
            Model model) {
        cartService.changeItemCount(id, action);

        List<ItemDto> items = cartService.getCartItems();
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.getTotal());
        return "cart";
    }
}
