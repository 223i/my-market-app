package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.CartItem;
import com.iron.mymarket.dao.entities.Item;
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

import java.util.Arrays;
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
                .flatMap(order ->
                        orderItemRepository.findAllByOrderId(order.getId())
                                .collectList()
                                .flatMap(items -> buildOrderDtoWithItems(order))
                );
    }

    public Mono<OrderDto> findOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Order not found: " + id)
                ))
                .flatMap(this::buildOrderDtoWithItems);
    }

    public Mono<OrderDto> findOrderByIdAndUserId(Long id, Long userId) {
        log.info("Search for order with id '{}' and userId '{}'", id, userId);
        return orderRepository.findByIdAndUserId(id, userId)
                .flatMap(this::buildOrderDtoWithItems)
                .switchIfEmpty(Mono.empty());
    }

    public Mono<OrderDto> createNewOrderWithPayment(Long userId) {
        // 1. Получаем актуальные товары из БД вместо CartStorage
        return cartRepository.findAllByUserId(userId).collectList()
                .flatMap(cartItems -> {
                    log.info("Cart items by userId: {}", Arrays.toString(cartItems.toArray()));
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty"));
                    }

                    // 2. Превращаем товары корзины в позиции заказа
                    return createOrderItemsFromCart(cartItems).collectList()
                            .flatMap(items -> {
                                long total = calculateOrderTotal(items);

                                // Сохраняем заказ в транзакции
                                return transactionalOperator.transactional(calculateTotalAndSaveOrder(items, userId))
                                        .flatMap(orderDto ->
                                                // Оплата после успешного сохранения заказа
                                                paymentClientService.pay((double) total)
                                                        .onErrorMap(paymentError -> {
                                                            log.error("Payment failed for order {}: {}", orderDto.getId(), paymentError.getMessage());
                                                            return new RuntimeException("Payment failed: " + paymentError.getMessage(), paymentError);
                                                        })
                                                        .thenReturn(orderDto)
                                        );
                            });
                })
                // 5. Очищаем корзину в БД только после успешной оплаты
                .flatMap(orderDto -> cartRepository.deleteAllByUserId(userId)
                        .thenReturn(orderDto));
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

    private Flux<OrderItem> createOrderItemsFromCart(List<CartItem> cartItems) {
        return Flux.fromIterable(cartItems)
                .flatMap(entry ->
                        itemRepository.findById(entry.getItemId())
                                .switchIfEmpty(Mono.error(
                                        new IllegalArgumentException("Item not found: " + entry.getItemId())
                                ))
                                .map(item -> createOrderItem(item, entry.getQuantity()))
                );
    }

    private OrderItem createOrderItem(Item item, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(item.getId());
        orderItem.setQuantity(quantity);
        orderItem.setPriceAtPurchase(item.getPrice());
        return orderItem;
    }

    private Mono<OrderDto> calculateTotalAndSaveOrder(List<OrderItem> orderItems, Long userId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalSum(calculateOrderTotal(orderItems));
        return saveOrderWithItems(order, orderItems);
    }

    private long calculateOrderTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    private Mono<OrderDto> saveOrderWithItems(Order order, List<OrderItem> orderItems) {
         // 1. Сначала сохраняем сам заказ, чтобы получить его ID из БД
        return orderRepository.save(order)
                .flatMap(savedOrder -> {
                    // 2. Проходим по всем позициям и устанавливаем ID сохраненного заказа
                    orderItems.forEach(item -> item.setOrderId(savedOrder.getId()));
                    return saveOrderItemsAndBuildDto(savedOrder, orderItems);
                });
    }

    private Mono<OrderDto> saveOrderItemsAndBuildDto(Order savedOrder, List<OrderItem> orderItems) {
        return orderItemRepository.saveAll(orderItems)
                .collectList() // ЖДЕМ, пока сохранятся ВЫ ВСЕ позиции заказа
                .flatMap(savedItems -> {
                    // Теперь, когда всё в базе, обогащаем данными из itemRepository
                    return Flux.fromIterable(savedItems)
                            .flatMap(savedItem ->
                                    itemRepository.findById(savedItem.getItemId())
                                            .map(item -> itemMapper.toOrderItemDto(savedItem, item))
                            )
                            .collectList()
                            .map(orderItemDtos -> orderMapper.toOrderDto(savedOrder, orderItemDtos));
                });
    }
}
