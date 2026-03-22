package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.CartItem;
import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.entities.OrderItem;
import com.iron.mymarket.dao.repository.CartRepository;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.OrderItemRepository;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.model.OrderItemDto;
import com.iron.mymarket.util.ItemMapper;
import com.iron.mymarket.util.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final TransactionalOperator transactionalOperator;
    private final PaymentClientService paymentClientService;
    private final CartRepository cartRepository;


    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        OrderMapper orderMapper,
                        ItemRepository itemRepository, ItemMapper itemMapper,
                        TransactionalOperator transactionalOperator,
                        PaymentClientService paymentClientService,
                        CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.transactionalOperator = transactionalOperator;
        this.paymentClientService = paymentClientService;
        this.cartRepository = cartRepository;
    }


    public Flux<OrderDto> findAllOrdersByUserId(Long userId) {
        log.info("Find all orders by user id: {}", userId);
        return orderRepository.findAllByUserId(userId)
                .flatMap(this::buildOrderDtoWithItems);
    }

    public Mono<OrderDto> findOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("Order not found: " + id)))
                .flatMap(this::buildOrderDtoWithItems);
    }

    public Mono<OrderDto> findOrderByIdAndUserId(Long id, Long userId) {
        log.info("Search for order with id '{}' and userId '{}'", id, userId);
        return orderRepository.findByIdAndUserId(id, userId)
                .flatMap(this::buildOrderDtoWithItems);
    }

    /**
     * Основной флоу создания заказа: Корзина -> Позиции -> Сохранение -> Оплата -> Очистка
     */
    public Mono<OrderDto> createNewOrderWithPayment(Long userId) {
        return cartRepository.findAllByUserId(userId)
                .collectList()
                .flatMap(this::ensureCartIsNotEmpty)
                .flatMap(cartItems -> convertCartToOrderItems(cartItems).collectList())
                .flatMap(orderItems -> processOrderCreation(orderItems, userId))
                .flatMap(orderDto -> executePayment(orderDto)
                        .then(Mono.defer(() -> clearUserCart(userId)))
                        .thenReturn(orderDto));
    }

    // Приватные методы для декомпозиции логики

    private Mono<List<CartItem>> ensureCartIsNotEmpty(List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            return Mono.error(new IllegalStateException("Cart is empty"));
        }
        return Mono.just(cartItems);
    }

    private Mono<OrderDto> processOrderCreation(List<OrderItem> orderItems, Long userId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalSum(calculateOrderTotal(orderItems));

        // Оборачиваем только сохранение в транзакцию
        return transactionalOperator.transactional(saveOrderWithItems(order, orderItems));
    }

    private Mono<Void> executePayment(OrderDto orderDto) {
        return paymentClientService.pay((double) orderDto.getTotalSum())
                .onErrorMap(e -> {
                    log.error("Payment failed for order {}: {}", orderDto.getId(), e.getMessage());
                    return new RuntimeException("Payment failed: " + e.getMessage(), e);
                })
                .then();
    }

    private Mono<Void> clearUserCart(Long userId) {
        return cartRepository.deleteAllByUserId(userId);
    }

    private Mono<OrderDto> buildOrderDtoWithItems(Order order) {
        return fetchOrderItemsWithDetails(order.getId())
                .map(items -> orderMapper.toOrderDto(order, items));
    }

    private Mono<List<OrderItemDto>> fetchOrderItemsWithDetails(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId)
                .flatMap(item -> itemRepository.findById(item.getItemId())
                        .map(details -> itemMapper.toOrderItemDto(item, details)))
                .collectList();
    }

    private Flux<OrderItem> convertCartToOrderItems(List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .flatMap(cartItem -> itemRepository.findById(cartItem.getItemId())
                        .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("Item not found: " + cartItem.getItemId())))
                        .map(item -> {
                            OrderItem oi = new OrderItem();
                            oi.setItemId(item.getId());
                            oi.setQuantity(cartItem.getQuantity());
                            oi.setPriceAtPurchase(item.getPrice());
                            return oi;
                        }));
    }

    private Mono<OrderDto> saveOrderWithItems(Order order, List<OrderItem> orderItems) {
        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    orderItems.forEach(oi -> oi.setOrderId(savedOrder.getId()));
                    return orderItemRepository.saveAll(orderItems)
                            .collectList()
                            .flatMap(items -> buildOrderDtoWithItems(savedOrder));
                });
    }

    private long calculateOrderTotal(List<OrderItem> orderItems) {
        return orderItems.stream().mapToLong(OrderItem::getSubtotal).sum();
    }
}
