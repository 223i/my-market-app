package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.model.Paging;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.ItemService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
public class ItemsController {

    private final ItemService itemService;
    private final CartService cartService;

    public ItemsController(ItemService itemService, CartService cartService) {
        this.itemService = itemService;
        this.cartService = cartService;
    }

    @GetMapping({"/", "/items"})
    public Mono<Rendering> getItems(@RequestParam(value = "search", defaultValue = "") String search,
                                    @RequestParam(value = "sort", defaultValue = "NO") ItemSort sort,
                                    @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
                                    @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize) {

        Flux<ItemDto> items = itemService.findItems(search, sort, pageNumber, pageSize + 1);

        return items.collectList()
                .flatMap(itemsDto -> {
                    boolean hasNext = itemsDto.size() > pageSize;
                    List<ItemDto> pageItems = itemsDto.stream().limit(pageSize).toList();
                    return Mono.just(Rendering.view("items")
                            .modelAttribute("items", toRows(Flux.fromIterable(pageItems), 3))
                            .modelAttribute("search", search)
                            .modelAttribute("sort", sort)
                            .modelAttribute("paging", new Paging(
                                    pageSize,
                                    pageNumber,
                                    pageNumber > 1,
                                    hasNext
                            ))
                            .build());
                });
    }

    @PostMapping("/items")
    public Mono<Rendering> postItemNumberInCart(@RequestParam Long id,
                                                @RequestParam ItemAction action,
                                                @RequestParam(value = "search", defaultValue = "", required = false) String search,
                                                @RequestParam(value = "sort", defaultValue = "NO") ItemSort sort,
                                                @RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber,
                                                @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                                                WebSession session) {
        session.getAttributes().putIfAbsent("cart", new CartStorage());
        CartStorage cart = (CartStorage) session.getAttributes().get("cart");

        return  cartService.changeItemCount(id, action, cart)
                .then(Mono.just(
                        Rendering.redirectTo("/items?" +
                                        "search=" + search +
                                        "&sort=" + sort +
                                        "&pageNumber=" + pageNumber +
                                        "&pageSize=" + pageSize)
                                .build()
                ));
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> getItemById(@PathVariable Long id) {
        return itemService.getItemById(id)
                .map(itemDto -> Rendering.view("item")
                        .modelAttribute("item", itemDto)
                        .build());
    }

    private Flux<List<ItemDto>> toRows(Flux<ItemDto> items, int rowSize) {

        return items.buffer(rowSize)
                .map(row -> {
                    while (row.size() < rowSize) {
                        row.add(ItemDto.stub());
                    }
                    return row;
                });
    }
}
