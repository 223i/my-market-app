package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.*;
import com.iron.mymarket.dao.repository.CartRepository;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.OrderItemRepository;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.model.OrderItemDto;
import com.iron.mymarket.util.ItemMapper;
import com.iron.mymarket.util.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private PaymentClientService paymentClientService;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private Item testItem;
    private CartItem testCartItem;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Setup test data
        testUser = new User("user123", "test@example.com", "Test User");
        testItem = new Item();
        testItem.setId(1L);
        testItem.setTitle("Test Item");
        testItem.setPrice(100L);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setUserId(1L);
        testCartItem.setItemId(1L);
        testCartItem.setQuantity(2);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setTotalSum(200L);
    }

    @Test
    void findAllOrdersByUserId_withValidUserId_shouldReturnUserOrders() {
        List<OrderItem> orderItems = List.of(
                createOrderItem(1L, 1L, 2, 100L),
                createOrderItem(2L, 2L, 1, 150L)
        );
        List<OrderItemDto> orderItemDtos = List.of(
                new OrderItemDto(new com.iron.mymarket.model.ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0), 2, 200L),
                new OrderItemDto(new com.iron.mymarket.model.ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 150, 0), 1, 150L)
        );
        OrderDto expectedOrderDto = new OrderDto(1L, 1L, orderItemDtos, 350L);

        when(orderRepository.findAllByUserId(1L)).thenReturn(Flux.just(testOrder));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Flux.fromIterable(orderItems));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(testItem));
        when(itemMapper.toOrderItemDto(any(OrderItem.class), any(Item.class)))
                .thenReturn(orderItemDtos.get(0), orderItemDtos.get(1));
        when(orderMapper.toOrderDto(eq(testOrder), any(List.class))).thenReturn(expectedOrderDto);

        // When
        Flux<OrderDto> result = orderService.findAllOrdersByUserId(1L);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedOrderDto)
                .verifyComplete();

        verify(orderRepository, times(1)).findAllByUserId(1L);
        verify(orderItemRepository, times(1)).findAllByOrderId(1L);
        verify(itemRepository, times(2)).findById(anyLong());
        verify(orderMapper, times(1)).toOrderDto(eq(testOrder), any(List.class));
    }

    @Test
    void findOrderById_existingId_shouldReturnMappedOrder() {
        OrderItem orderItem = createOrderItem(1L, 1L, 1, 100L);
        OrderItemDto orderItemDto = new OrderItemDto(
                new com.iron.mymarket.model.ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                1, 100L
        );
        OrderDto expectedOrderDto = new OrderDto(1L, 1L, List.of(orderItemDto), 100L);

        when(orderRepository.findById(1L)).thenReturn(Mono.just(testOrder));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Flux.just(orderItem));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemMapper.toOrderItemDto(orderItem, testItem)).thenReturn(orderItemDto);
        when(orderMapper.toOrderDto(testOrder, List.of(orderItemDto))).thenReturn(expectedOrderDto);

        Mono<OrderDto> result = orderService.findOrderById(1L);

        StepVerifier.create(result)
                .expectNext(expectedOrderDto)
                .verifyComplete();

        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).findAllByOrderId(1L);
        verify(itemRepository, times(1)).findById(1L);
        verify(orderMapper, times(1)).toOrderDto(testOrder, List.of(orderItemDto));
    }

    @Test
    void findOrderById_nonExistingId_shouldThrowException() {
        when(orderRepository.findById(1L)).thenReturn(Mono.empty());

        Mono<OrderDto> result = orderService.findOrderById(1L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Order not found"))
                .verify();

        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void findOrderByIdAndUserId_withValidOrderAndUser_shouldReturnOrder() {
        OrderItem orderItem = createOrderItem(1L, 1L, 1, 100L);
        OrderItemDto orderItemDto = new OrderItemDto(
                new com.iron.mymarket.model.ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                1, 100L
        );
        OrderDto expectedOrderDto = new OrderDto(1L, 1L, List.of(orderItemDto), 100L);

        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Mono.just(testOrder));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Flux.just(orderItem));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(itemMapper.toOrderItemDto(orderItem, testItem)).thenReturn(orderItemDto);
        when(orderMapper.toOrderDto(testOrder, List.of(orderItemDto))).thenReturn(expectedOrderDto);

        Mono<OrderDto> result = orderService.findOrderByIdAndUserId(1L, 1L);

        StepVerifier.create(result)
                .expectNext(expectedOrderDto)
                .verifyComplete();

        verify(orderRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(orderItemRepository, times(1)).findAllByOrderId(1L);
        verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    void createNewOrderWithPayment_withValidCart_shouldCreateOrderAndProcessPayment() {
        List<CartItem> cartItems = List.of(testCartItem);
        OrderItem orderItem = createOrderItem(1L, 1L, 2, 100L);
        OrderItemDto orderItemDto = new OrderItemDto(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                2, 200L
        );
        OrderDto expectedOrderDto = new OrderDto(1L, 1L, List.of(orderItemDto), 200L);

        when(cartRepository.findAllByUserId(1L)).thenReturn(Flux.fromIterable(cartItems));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(testOrder));
        when(orderItemRepository.saveAll(any(List.class))).thenReturn(Flux.just(orderItem));
        when(orderItemRepository.findAllByOrderId(any(Long.class))).thenReturn(Flux.just(orderItem));
        when(itemMapper.toOrderItemDto(any(OrderItem.class), any(Item.class))).thenReturn(orderItemDto);
        when(orderMapper.toOrderDto(any(Order.class), any(List.class))).thenReturn(expectedOrderDto);
        when(paymentClientService.pay(200.0)).thenReturn(Mono.just(800.0));
        when(cartRepository.deleteAllByUserId(1L)).thenReturn(Mono.empty());

        Mono<OrderDto> result = orderService.createNewOrderWithPayment(1L);

        StepVerifier.create(result)
                .expectNext(expectedOrderDto)
                .verifyComplete();

        verify(cartRepository, times(1)).findAllByUserId(1L);
        verify(itemRepository, times(2)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(any(List.class));
        verify(paymentClientService, times(1)).pay(200.0);
        verify(cartRepository, times(1)).deleteAllByUserId(1L);
    }

    @Test
    void createNewOrderWithPayment_withEmptyCart_shouldThrowException() {
        when(cartRepository.findAllByUserId(1L)).thenReturn(Flux.empty());

        Mono<OrderDto> result = orderService.createNewOrderWithPayment(1L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException &&
                        throwable.getMessage().equals("Cart is empty"))
                .verify();

        verify(cartRepository, times(1)).findAllByUserId(1L);
        verifyNoInteractions(itemRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentClientService);
    }

    @Test
    void createNewOrderWithPayment_withPaymentFailure_shouldNotClearCart() {
        List<CartItem> cartItems = List.of(testCartItem);
        OrderItem orderItem = createOrderItem(1L, 1L, 2, 100L);
        OrderItemDto orderItemDto = new OrderItemDto(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                2, 200L
        );
        OrderDto expectedOrderDto = new OrderDto(1L, 1L, List.of(orderItemDto), 200L);

        when(cartRepository.findAllByUserId(1L)).thenReturn(Flux.fromIterable(cartItems));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(testItem));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(testOrder));
        when(orderItemRepository.saveAll(any(List.class))).thenReturn(Flux.just(orderItem));
        when(orderItemRepository.findAllByOrderId(any(Long.class))).thenReturn(Flux.just(orderItem));
        when(orderMapper.toOrderDto(any(), any())).thenReturn(expectedOrderDto);
        when(itemMapper.toOrderItemDto(any(), any())).thenReturn(orderItemDto);
        when(paymentClientService.pay(any(Double.class))).thenReturn(Mono.error(new RuntimeException("Payment failed")));

        Mono<OrderDto> result = orderService.createNewOrderWithPayment(1L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Payment failed"))
                .verify();

        verify(cartRepository, times(1)).findAllByUserId(1L);
        verify(itemRepository, times(2)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).saveAll(any(List.class));
        verify(paymentClientService, times(1)).pay(200.0);
        verify(cartRepository, never()).deleteAllByUserId(1L); // Cart should not be cleared on payment failure
    }

    private OrderItem createOrderItem(Long id, Long itemId, int quantity, long price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setItemId(itemId);
        orderItem.setQuantity(quantity);
        orderItem.setPriceAtPurchase(price);
        return orderItem;
    }
}
