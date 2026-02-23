package com.iron.mymarket.dao.repository;

import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.model.OrderDto;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;


public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    Mono<OrderDto> findOrderById(Long id);
}
