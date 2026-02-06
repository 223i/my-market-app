package com.iron.mymarket.controller;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.model.Paging;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
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
        List<Integer> pageSizes = List.of(2, 5, 10, 20, 50, 100);

        return items.collectList()
                .defaultIfEmpty(Collections.emptyList())
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
                            .modelAttribute("pageSizes", pageSizes)
                            .build());
                });
    }

    @PostMapping("/items")
    public Mono<Rendering> postItemNumberInCart(ServerWebExchange exchange, WebSession session) {

        return exchange.getFormData().flatMap(formData -> {
            Long id = Long.valueOf(Objects.requireNonNull(formData.getFirst("id")));
            ItemAction action = ItemAction.valueOf(formData.getFirst("action"));
            CartStorage cart = session.getAttributeOrDefault("cart", new CartStorage());

            return cartService.changeItemCount(id, action, cart)
                    .flatMap(updatedCart -> {
                        session.getAttributes().put("cart", updatedCart);
                        return session.save();
                    })
                    .then(Mono.just(Rendering.redirectTo(getRedirectUri(formData).toString()).build()));
        });
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

    private URI getRedirectUri(MultiValueMap<String, String> formData) {
        return UriComponentsBuilder
                .fromPath("/items")
                .queryParam("search", formData.getFirst("search"))
                .queryParam("sort", formData.getFirst("sort"))
                .queryParam("pageNumber", formData.getFirst("pageNumber"))
                .queryParam("pageSize", formData.getFirst("pageSize"))
                .build()
                .toUri();
    }
}
