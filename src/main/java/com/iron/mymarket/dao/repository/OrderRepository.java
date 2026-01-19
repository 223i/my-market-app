package com.iron.mymarket.dao.repository;

import com.iron.mymarket.dao.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderRepository extends JpaRepository<Order, Long> {

}
