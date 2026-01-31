package com.iron.mymarket.dao.repository;

import com.iron.mymarket.dao.entities.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;


public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

}
