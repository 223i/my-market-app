package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.entities.OrderItem;
import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.util.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ItemRepository itemRepository;
    private final CartStorage cartStorage;
    private final TransactionalOperator transactionalOperator;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper,
                        ItemRepository itemRepository, CartStorage cartStorage,
                        TransactionalOperator transactionalOperator) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.itemRepository = itemRepository;
        this.cartStorage = cartStorage;
        this.transactionalOperator = transactionalOperator;
    }

    public Flux<OrderDto> findOrders() {
        return orderRepository.findAll()
                        .map(orderMapper::toOrderDto);

    }

    public Mono<OrderDto> findOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found:" + id)))
                .map(orderMapper::toOrderDto);
    }

    public Mono<OrderDto> createNewOrder() {
        Map<Long, Integer> cartItems = cartStorage.getItems();

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Order order = new Order();
        return Flux.fromIterable(cartItems.entrySet())
                .flatMap(entry ->
                        itemRepository.findById(entry.getKey())
                                .switchIfEmpty(Mono.error(
                                        new IllegalArgumentException("Item not found: " + entry.getKey())
                                ))
                                .map(item -> {
                                    OrderItem orderItem = new OrderItem();
                                    orderItem.setItem(item);
                                    orderItem.setQuantity(entry.getValue());
                                    orderItem.setPriceAtPurchase(item.getPrice());
                                    orderItem.setOrder(order);
                                    return orderItem;
                                })
                )
                .collectList()
                .flatMap(items -> {
                    order.setItems(items);
                    order.setTotalSum(
                            items.stream()
                                    .mapToLong(OrderItem::getSubtotal)
                                    .sum()
                    );
                    return orderRepository.save(order);
                })
                .map(orderMapper::toOrderDto)
                .as(transactionalOperator::transactional)
                .doFinally(signal -> cartStorage.getItems().clear());
    }
}
