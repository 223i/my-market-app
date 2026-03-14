package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.model.Paging;
import com.iron.mymarket.service.CartService;
import com.iron.mymarket.service.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
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
                                    @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                                    @RequestParam(value = "logout", required = false) String logout) {

        Flux<ItemDto> items = itemService.findItems(search, sort, pageNumber, pageSize + 1);
        List<Integer> pageSizes = List.of(2, 5, 10, 20, 50, 100);

        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated())
                .defaultIfEmpty(false)
                .zipWith(items.collectList().defaultIfEmpty(Collections.emptyList()))
                .flatMap(tuple -> {
                    boolean isAuthenticated = tuple.getT1();
                    List<ItemDto> itemsList = tuple.getT2();
                    boolean hasNext = itemsList.size() > pageSize;
                    List<ItemDto> pageItems = itemsList.stream().limit(pageSize).toList();
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
                            .modelAttribute("isAuthenticated", isAuthenticated)
                            .modelAttribute("logout", logout != null)
                            .build());
                });
    }

    @PostMapping("/items")
    public Mono<Rendering> postItemNumberInCart(ServerWebExchange exchange,
                                                @AuthenticationPrincipal OAuth2User principal) {
        // Если путь permitAll, principal может быть null
        if (principal == null) {
            return Mono.just(Rendering.redirectTo("/auth/login").build());
        }

        String externalId = principal.getAttribute("sub");

        return exchange.getFormData().flatMap(formData -> {
            Long id = Long.valueOf(Objects.requireNonNull(formData.getFirst("id")));
            ItemAction action = ItemAction.valueOf(formData.getFirst("action"));

            return cartService.changeItemCountByExternalId(id, action, externalId)
                    .then(Mono.just(Rendering.redirectTo(getRedirectUri(formData).toString()).build()));
        });
    }


    @PostMapping("/items/{id}")
    public Mono<Rendering> postItemById(@PathVariable Long id, ServerWebExchange exchange,
                                        @AuthenticationPrincipal OAuth2User principal) {

        // Если путь permitAll, principal может быть null
        if (principal == null) {
            return Mono.just(Rendering.redirectTo("/auth/login").build());
        }

        // Извлекаем наш ID, добавленный в сервисе выше
        Long userId = principal.getAttribute("internal_id");

        return exchange.getFormData().flatMap(formData -> {
            ItemAction action = ItemAction.valueOf(formData.getFirst("action"));

            // Просто вызываем сервис, передавая уже готовый Long userId
            return cartService.changeItemCount(id, action, userId)
                    .thenReturn(Rendering.redirectTo("/items/" + id).build());
        });
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> getItemById(@PathVariable Long id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated())
                .defaultIfEmpty(false)
                .zipWith(itemService.getItemById(id))
                .flatMap(tuple -> {
                    boolean isAuthenticated = tuple.getT1();
                    return Mono.just(Rendering.view("item")
                            .modelAttribute("item", tuple.getT2())
                            .modelAttribute("isAuthenticated", isAuthenticated)
                            .build());
                });
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
