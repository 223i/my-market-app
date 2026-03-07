package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.entities.OrderItem;
import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.OrderItemRepository;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.model.OrderItemDto;
import com.iron.mymarket.util.ItemMapper;
import com.iron.mymarket.util.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;


@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final TransactionalOperator transactionalOperator;
    private final PaymentClientService paymentClientService;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, OrderMapper orderMapper,
                        ItemRepository itemRepository, ItemMapper itemMapper,
                        TransactionalOperator transactionalOperator, PaymentClientService paymentClientService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.transactionalOperator = transactionalOperator;
        this.paymentClientService = paymentClientService;
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

    public Mono<OrderDto> createNewOrderWithPayment(CartStorage cartStorage) {
        return validateCart(cartStorage)
                .flatMap(cartItems -> createOrderItemsFromCart(cartItems).collectList())
                .flatMap(items -> {
                    long total = calculateOrderTotal(items);

                    // 1. Проверяем баланс ДО транзакции (внешний вызов)
                    return paymentClientService.getBalance()
                            .flatMap(balance -> {
                                if (balance < total) {
                                    return Mono.error(new RuntimeException("Insufficient funds"));
                                }

                                // 2. Выполняем БД-операции внутри транзакции
                                return transactionalOperator.execute(status ->
                                        calculateTotalAndSaveOrder(items)
                                ).next(); // Flux -> Mono
                            })
                            // 3. После успешного сохранения в БД вызываем сервис оплаты
                            .flatMap(orderDto ->
                                    paymentClientService.pay((double) total)
                                            .thenReturn(orderDto)
                            );
                })
                // Очищаем корзину только после успешного завершения всей цепочки
                .doOnNext(dto -> cartStorage.getItems().clear());
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

    private Mono<List<OrderItemDto>> fetchOrderItemsWithDetails(Long orderId) {
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

    private Mono<OrderDto> calculateTotalAndSaveOrder(List<OrderItem> orderItems) {
        Order order = new Order();
        long totalSum = calculateOrderTotal(orderItems);
        order.setTotalSum(totalSum);

        return saveOrderWithItems(order, orderItems);
    }

    private long calculateOrderTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    private Mono<OrderDto> saveOrderWithItems(Order order, List<OrderItem> orderItems) {
        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    orderItems.forEach(item -> item.setOrderId(savedOrder.getId()));
                    return saveOrderItemsAndBuildDto(savedOrder, orderItems);
                });
    }

    private Mono<OrderDto> saveOrderItemsAndBuildDto(Order savedOrder, List<OrderItem> orderItems) {
        return orderItemRepository.saveAll(orderItems)
                .flatMap(savedItem ->
                        itemRepository.findById(savedItem.getItemId())
                                .map(item -> itemMapper.toOrderItemDto(savedItem, item))
                )
                .collectList()
                .map(orderItemDtos -> orderMapper.toOrderDto(savedOrder, orderItemDtos));
    }
}
