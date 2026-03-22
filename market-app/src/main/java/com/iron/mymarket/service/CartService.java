package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.CartItem;
import com.iron.mymarket.dao.repository.CartRepository;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.UserRepository;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.util.ItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CacheService cacheService;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public Mono<ItemDto> getItemView(long itemId, long userId) {
        String cacheKey = "items:" + itemId;

        return cacheService.get(cacheKey)
                .cast(ItemDto.class)
                .switchIfEmpty(
                        itemRepository.findById(itemId)
                                .map(itemMapper::toItemDto)
                                .flatMap(dto -> cacheService.setWithExpiration(cacheKey, dto, CACHE_TTL)
                                        .thenReturn(dto))
                )
                .flatMap(dto -> cartRepository.findByUserIdAndItemId(userId, itemId)
                        .map(cartItem -> {
                            ItemDto view = dto.toBuilder().build();
                            view.setCount(cartItem.getQuantity());
                            return view;
                        })
                        .defaultIfEmpty(dto)
                );
    }

    public Flux<ItemDto> getCartItems(Long userId) {
        return cartRepository.findAllByUserId(userId)
                .flatMap(cartItem -> getItemView(cartItem.getItemId(), userId));
    }

    public Mono<Long> getTotal(Long userId) {
        return getCartItems(userId)
                .map(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public Mono<Void> changeItemCountByExternalId(Long itemId, ItemAction action, String externalId) {
        log.debug("DEBUG: changeItemCountByExternalId called for externalId: {}, itemId: {}", externalId, itemId);
        return userRepository.findByExternalId(externalId)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found with externalId: " + externalId)))
                .flatMap(user -> changeItemCount(itemId, action, user.getId()));
    }

    public Mono<Void> changeItemCount(Long itemId, ItemAction action, Long userId) {
        log.debug("DEBUG: changeItemCount called for userId: {}, itemId: {}", userId, itemId);
        return cartRepository.findByUserIdAndItemId(userId, itemId)
                .flatMap(cartItem -> {
                    switch (action) {
                        case PLUS -> cartItem.setQuantity(cartItem.getQuantity() + 1);
                        case MINUS -> {
                            if (cartItem.getQuantity() > 1) cartItem.setQuantity(cartItem.getQuantity() - 1);
                            else return cartRepository.delete(cartItem).then(Mono.empty());
                        }
                        case DELETE -> {
                            return cartRepository.delete(cartItem).then(Mono.empty());
                        }
                    }
                    return cartRepository.save(cartItem);
                })
                .switchIfEmpty(Mono.defer(() ->
                        action == ItemAction.PLUS
                                ? cartRepository.save(new CartItem(userId, itemId, 1))
                                : Mono.empty()
                ))
                .then();
    }
}

