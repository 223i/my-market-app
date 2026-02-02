package com.iron.mymarket.service;

import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.util.ItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public Mono<ItemDto> getItemView(long itemId, CartStorage cart) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found:" + itemId)))
                .map(itemMapper::toItemDto)
                .map(itemDto -> {
                    itemDto.setCount(cart.getCount(itemId));
                    return itemDto;
                });
    }

    public Flux<ItemDto> getCartItems(CartStorage cart) {
        return Flux.fromIterable(cart.getItems().keySet())
                .flatMap(itemId -> getItemView(itemId, cart));
    }

    public Mono<Long> getTotal(CartStorage cart) {
        return getCartItems(cart)
                .map(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public Mono<Void> changeItemCount(Long itemId, ItemAction action, CartStorage cart) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found:" + itemId)))
                .doOnNext(item -> {
                    switch (action) {
                        case PLUS -> cart.plus(item.getId());
                        case MINUS -> cart.minus(item.getId());
                        case DELETE -> cart.delete(item.getId());
                    }
                })
                .then();
    }
}

