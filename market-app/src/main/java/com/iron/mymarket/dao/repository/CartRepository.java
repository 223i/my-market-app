package com.iron.mymarket.dao.repository;

import com.iron.mymarket.dao.entities.CartItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartRepository extends ReactiveCrudRepository<CartItem, Long> {

    Flux<CartItem> findAllByUserId(Long userId);

    Mono<CartItem> findByUserIdAndItemId(Long userId, Long itemId);

    Mono<Void> deleteAllByUserId(Long userId);
}