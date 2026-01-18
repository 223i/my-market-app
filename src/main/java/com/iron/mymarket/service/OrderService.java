package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.entities.OrderItem;
import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.util.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ItemRepository itemRepository;
    private final CartStorage cartStorage;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper,
                        ItemRepository itemRepository, CartStorage cartStorage) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.itemRepository = itemRepository;
        this.cartStorage = cartStorage;
    }

    public List<OrderDto> findOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(orderMapper::toOrderDto).toList();
    }

    public OrderDto findOrderById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        return orderMapper.toOrderDto(order);
    }

    @Transactional
    public OrderDto createNewOrder() {
        Map<Long, Integer> cartItems = cartStorage.getItems();

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Order order = new Order();
        List<OrderItem> orderItems = new ArrayList<>();
        long totalSum = 0;

        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {
            Item item = itemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + entry.getKey()));

            OrderItem orderItem = new OrderItem();
            orderItem.setItem(item);
            orderItem.setQuantity(entry.getValue());
            orderItem.setPriceAtPurchase(item.getPrice());
            orderItem.setOrder(order);

            orderItems.add(orderItem);
            totalSum += orderItem.getSubtotal();
        }

        order.setItems(orderItems);
        order.setTotalSum(totalSum);

        Order savedOrder = orderRepository.save(order);

        // очистка корзины
        cartStorage.getItems().clear();
        return orderMapper.toOrderDto(savedOrder);
    }
}
