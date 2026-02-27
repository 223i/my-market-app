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
                .flatMap(this::buildOrderDtoWithItems);
    }

    public Mono<OrderDto> findOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Order not found: " + id)
                ))
                .flatMap(this::buildOrderDtoWithItems);
    }

    public Mono<OrderDto> createNewOrder(CartStorage cartStorage) {
        return validateCart(cartStorage)
                .flatMap(cartItems -> createOrderItemsFromCart(cartItems).collectList())
                .flatMap(this::calculateTotalAndSaveOrder)
                .as(transactionalOperator::transactional)
                .flatMap(dto -> {
                    cartStorage.getItems().clear();
                    return Mono.just(dto);
                });
    }

    private Mono<OrderDto> buildOrderDtoWithItems(Order order) {
        return fetchOrderItemsWithDetails(order.getId())
                .map(orderItemDtos -> orderMapper.toOrderDto(order, orderItemDtos));
    }

    private Mono<java.util.List<com.iron.mymarket.model.OrderItemDto>> fetchOrderItemsWithDetails(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId)
                .flatMap(this::buildOrderItemDtoWithItemDetails)
                .collectList();
    }

    private Mono<com.iron.mymarket.model.OrderItemDto> buildOrderItemDtoWithItemDetails(OrderItem orderItem) {
        return itemRepository.findById(orderItem.getItemId())
                .map(item -> itemMapper.toOrderItemDto(orderItem, item));
    }

    private Mono<Map<Long, Integer>> validateCart(CartStorage cartStorage) {
        Map<Long, Integer> cartItems = cartStorage.getItems();
        if (cartItems.isEmpty()) {
            return Mono.error(new IllegalStateException("Cart is empty"));
        }
        return Mono.just(cartItems);
    }

    private Flux<OrderItem> createOrderItemsFromCart(Map<Long, Integer> cartItems) {
        return Flux.fromIterable(cartItems.entrySet())
                .flatMap(entry ->
                        itemRepository.findById(entry.getKey())
                                .switchIfEmpty(Mono.error(
                                        new IllegalArgumentException("Item not found: " + entry.getKey())
                                ))
                                .map(item -> createOrderItem(item, entry.getValue()))
                );
    }

    private OrderItem createOrderItem(com.iron.mymarket.dao.entities.Item item, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(item.getId());
        orderItem.setQuantity(quantity);
        orderItem.setPriceAtPurchase(item.getPrice());
        return orderItem;
    }

    private Mono<OrderDto> calculateTotalAndSaveOrder(java.util.List<OrderItem> orderItems) {
        Order order = new Order();
        long totalSum = calculateOrderTotal(orderItems);
        order.setTotalSum(totalSum);

        return saveOrderWithItems(order, orderItems);
    }

    private long calculateOrderTotal(java.util.List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    private Mono<OrderDto> saveOrderWithItems(Order order, java.util.List<OrderItem> orderItems) {
        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    orderItems.forEach(item -> item.setOrderId(savedOrder.getId()));
                    return saveOrderItemsAndBuildDto(savedOrder, orderItems);
                });
    }

    private Mono<OrderDto> saveOrderItemsAndBuildDto(Order savedOrder, java.util.List<OrderItem> orderItems) {
        return orderItemRepository.saveAll(orderItems)
                .flatMap(savedItem ->
                        itemRepository.findById(savedItem.getItemId())
                                .map(item -> itemMapper.toOrderItemDto(savedItem, item))
                )
                .collectList()
                .map(orderItemDtos -> orderMapper.toOrderDto(savedOrder, orderItemDtos));
    }
}
