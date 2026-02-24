package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private CartStorage cartStorage;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionalOperator.transactional(any(Flux.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void findOrders_shouldReturnMappedOrders() {
        Order order1 = new Order();
        order1.setId(1L);
        Order order2 = new Order();
        order2.setId(2L);

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setOrderId(1L);
        orderItem1.setItemId(10L);
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOrderId(2L);
        orderItem2.setItemId(20L);

        Item item1 = new Item();
        item1.setId(10L);
        item1.setTitle("Product 1");
        Item item2 = new Item();
        item2.setId(20L);
        item2.setTitle("Product 2");

        OrderItemDto orderItemDto1 = new OrderItemDto();
        orderItemDto1.setItem(itemMapper.toItemDto(item1));
        OrderItemDto orderItemDto2 = new OrderItemDto();
        orderItemDto2.setItem(itemMapper.toItemDto(item2));

        when(orderRepository.findAll()).thenReturn(Flux.just(order1, order2));

        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Flux.just(orderItem1));
        when(orderItemRepository.findAllByOrderId(2L)).thenReturn(Flux.just(orderItem2));

        when(itemRepository.findById(10L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(20L)).thenReturn(Mono.just(item2));

        when(itemMapper.toOrderItemDto(orderItem1, item1)).thenReturn(orderItemDto1);
        when(itemMapper.toOrderItemDto(orderItem2, item2)).thenReturn(orderItemDto2);

        OrderDto dto1 = new OrderDto();
        dto1.setId(1L);
        OrderDto dto2 = new OrderDto();
        dto2.setId(2L);

        when(orderMapper.toOrderDto(eq(order1), anyList())).thenReturn(dto1);
        when(orderMapper.toOrderDto(eq(order2), anyList())).thenReturn(dto2);


        StepVerifier.create(orderService.findOrders())
                .assertNext(res -> assertEquals(1L, res.getId()))
                .assertNext(res -> assertEquals(2L, res.getId()))
                .verifyComplete();

        verify(orderRepository, times(1)).findAll();
        verify(orderItemRepository, times(2)).findAllByOrderId(any());
        verify(itemRepository, times(2)).findById(anyLong());
    }

    @Test
    void findOrderById_existingId_shouldReturnMappedOrder() {
        Order order1 = new Order();
        order1.setId(1L);
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setOrderId(1L);
        orderItem1.setItemId(10L);
        Item item1 = new Item();
        item1.setId(10L);
        item1.setTitle("Product 1");
        OrderItemDto orderItemDto1 = new OrderItemDto();
        orderItemDto1.setItem(itemMapper.toItemDto(item1));
        OrderDto dto1 = new OrderDto();
        dto1.setId(1L);


        when(orderRepository.findById(1L)).thenReturn(Mono.just(order1));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Flux.just(orderItem1));
        when(orderMapper.toOrderDto(order1, List.of(orderItemDto1))).thenReturn(dto1);
        when(itemRepository.findById(10L)).thenReturn(Mono.just(item1));
        when(itemMapper.toOrderItemDto(orderItem1, item1)).thenReturn(orderItemDto1);

        OrderDto result = orderService.findOrderById(1L).block();

        assertEquals(1L, result.getId());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderMapper, times(1)).toOrderDto(order1, List.of(orderItemDto1));
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
    void createNewOrder_withItems_shouldSaveOrderAndClearCart() {
        Map<Long, Integer> cartItems = new HashMap<>();
        cartItems.put(1L, 2);
        cartItems.put(2L, 1);

        when(cartStorage.getItems()).thenReturn(cartItems);

        Item item1 = new Item();
        item1.setId(1L);
        item1.setPrice(100L);
        Item item2 = new Item();
        item2.setId(2L);
        item2.setPrice(200L);

        when(itemRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id.equals(1L)) return Mono.just(item1);
            if (id.equals(2L)) return Mono.just(item2);
            return Mono.empty();
        });
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyIterable()))
                .thenReturn(Flux.just(new OrderItem()));

        OrderDto dtoMock = new OrderDto();
        dtoMock.setId(1L);
        when(orderItemRepository.save(any())).thenReturn(Mono.empty());
        when(orderMapper.toOrderDto(any(), anyList())).thenReturn(dtoMock);

        // Выполнение
        Mono<OrderDto> result = orderService.createNewOrder(cartStorage);

        StepVerifier.create(result)
                .assertNext(dto -> assertEquals(1L, dto.getId()))
                .verifyComplete();

        // Проверки
        assertTrue(cartItems.isEmpty(), "Корзина должна быть очищена после создания заказа");
        verify(orderRepository).save(any(Order.class));
        verify(itemRepository).findById(1L);
        verify(itemRepository).findById(2L);
    }

    @Test
    void createNewOrder_emptyCart_shouldThrowException() {
        when(cartStorage.getItems()).thenReturn(Collections.emptyMap());

        StepVerifier.create(orderService.createNewOrder(cartStorage))
                .expectErrorMatches(throwable -> throwable instanceof IllegalStateException &&
                        throwable.getMessage().equals("Cart is empty"))
                .verify();

        verifyNoInteractions(itemRepository);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createNewOrder_itemNotFound_shouldThrowException() {
        Map<Long, Integer> cartItems = Map.of(1L, 1);
        when(cartStorage.getItems()).thenReturn(cartItems);

        when(itemRepository.findById(1L)).thenReturn(Mono.empty());

        Mono<OrderDto> result = orderService.createNewOrder(cartStorage);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Item not found"))
                .verify();

        verify(itemRepository).findById(1L);
        verifyNoInteractions(orderRepository);
    }
}
