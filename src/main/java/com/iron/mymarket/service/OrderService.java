package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.entities.OrderItem;
import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.OrderItemRepository;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.util.ItemMapper;
import com.iron.mymarket.util.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final TransactionalOperator transactionalOperator;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, OrderMapper orderMapper,
                        ItemRepository itemRepository, ItemMapper itemMapper,
                        TransactionalOperator transactionalOperator) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.transactionalOperator = transactionalOperator;
    }

    public Flux<OrderDto> findOrders() {
        return orderRepository.findAll()
                .flatMap(order ->
                        orderItemRepository.findAllByOrderId(order.getId())
                                .flatMap(orderItem ->
                                        itemRepository.findById(orderItem.getItemId())
                                                .map(item ->
                                                        itemMapper.toOrderItemDto(orderItem, item)
                                                )
                                )
                                .collectList()
                                .map(orderItemDtos ->
                                        orderMapper.toOrderDto(order, orderItemDtos)
                                )
                );
    }

    public Mono<OrderDto> findOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Order not found: " + id)
                ))
                .flatMap(order ->
                        orderItemRepository.findAllByOrderId(order.getId())
                                .flatMap(orderItem ->
                                        itemRepository.findById(orderItem.getItemId())
                                                .map(item ->
                                                        itemMapper.toOrderItemDto(orderItem, item)
                                                )
                                )
                                .collectList()
                                .map(orderItemDtos ->
                                        orderMapper.toOrderDto(order, orderItemDtos)
                                )
                );
    }

    public Mono<OrderDto> createNewOrder(CartStorage cartStorage) {
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
                                    orderItem.setItemId(item.getId());
                                    orderItem.setQuantity(entry.getValue());
                                    orderItem.setPriceAtPurchase(item.getPrice());
                                    orderItem.setOrderId(order.getId());
                                    return orderItem;
                                })
                )
                .collectList()
                .flatMap(items -> {
                    // считаем сумму
                    long totalSum = items.stream()
                            .mapToLong(OrderItem::getSubtotal)
                            .sum();

                    order.setTotalSum(totalSum);

                    // сохраняем order
                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {
                                // проставляем orderId (если id генерится БД)
                                items.forEach(i -> i.setOrderId(savedOrder.getId()));

                                return orderItemRepository.saveAll(items)
                                        .flatMap(savedItem ->
                                                itemRepository.findById(savedItem.getItemId())
                                                        .map(item ->
                                                                itemMapper.toOrderItemDto(savedItem, item)
                                                        )
                                        ).collectList()
                                        .map(orderItemDtos ->
                                                orderMapper.toOrderDto(savedOrder, orderItemDtos));
                            });
                })
                .as(transactionalOperator::transactional)
                .doFinally(signal -> cartStorage.getItems().clear());
    }
}
