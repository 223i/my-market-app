package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.util.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    public List<OrderDto> findOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(orderMapper::toOrderDto).toList();
    }

    public OrderDto findOrderById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        return orderMapper.toOrderDto(order);
    }


}
