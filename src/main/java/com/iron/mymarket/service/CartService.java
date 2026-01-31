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
    private final CartStorage cartStorage;
    private final ItemMapper itemMapper;

    public Mono<ItemDto> getItemView(long itemId) {
        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found:" + itemId)))
                .map(itemMapper::toItemDto)
                .map(itemDto -> {
                    itemDto.setCount(cartStorage.getCount(itemId));
                    return itemDto;
                });
    }

    public Flux<ItemDto> getCartItems() {
        return Flux.fromIterable(cartStorage.getItems().keySet())
                .flatMap(this::getItemView);
    }

    public Mono<Long> getTotal() {
        return getCartItems()
                .map(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public void changeItemCount(Mono<Long> itemId, ItemAction action) {
        itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found:" + itemId)))
                .doOnNext(item -> {
                    switch (action) {
                        case PLUS -> cartStorage.plus(item.getId());
                        case MINUS -> cartStorage.minus(item.getId());
                        case DELETE -> cartStorage.delete(item.getId());
                    }
                });
    }
}

