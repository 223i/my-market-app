package com.iron.mymarket.dao.repository;

import com.iron.mymarket.dao.entities.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Mono<Order> findByIdAndUserId(Long id, Long userId);

    Flux<Order> findAllByUserId(Long userId);
}
