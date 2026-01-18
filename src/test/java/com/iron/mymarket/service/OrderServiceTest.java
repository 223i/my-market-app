package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.dao.repository.OrderRepository;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.util.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartStorage cartStorage;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findOrders_shouldReturnMappedOrders() {
        Order order1 = new Order();
        order1.setId(1L);
        Order order2 = new Order();
        order2.setId(2L);

        List<Order> orders = List.of(order1, order2);
        when(orderRepository.findAll()).thenReturn(orders);

        OrderDto dto1 = new OrderDto();
        dto1.setId(1L);
        OrderDto dto2 = new OrderDto();
        dto2.setId(2L);

        when(orderMapper.toOrderDto(order1)).thenReturn(dto1);
        when(orderMapper.toOrderDto(order2)).thenReturn(dto2);

        List<OrderDto> result = orderService.findOrders();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void findOrderById_existingId_shouldReturnMappedOrder() {
        Order order = new Order();
        order.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        OrderDto dto = new OrderDto();
        dto.setId(1L);
        when(orderMapper.toOrderDto(order)).thenReturn(dto);

        OrderDto result = orderService.findOrderById(1L);

        assertEquals(1L, result.getId());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderMapper, times(1)).toOrderDto(order);
    }

    @Test
    void findOrderById_nonExistingId_shouldThrowException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> {
            orderService.findOrderById(1L);
        });

        assertTrue(ex.getMessage().contains("Order not found"));
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void createNewOrder_withItems_shouldSaveOrderAndClearCart() {
        Map<Long, Integer> cartItems = new HashMap<>();
        cartItems.put(1L, 2); // 2 шт.
        cartItems.put(2L, 1); // 1 шт.

        when(cartStorage.getItems()).thenReturn(cartItems);

        Item item1 = new Item();
        item1.setId(1L);
        item1.setPrice(100);

        Item item2 = new Item();
        item2.setId(2L);
        item2.setPrice(200);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDto dto = new OrderDto();
        dto.setId(1L);
        when(orderMapper.toOrderDto(savedOrder)).thenReturn(dto);

        OrderDto result = orderService.createNewOrder();

        assertEquals(1L, result.getId());
        assertTrue(cartItems.isEmpty()); // корзина очищена

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).findById(2L);
        verify(cartStorage, times(2)).getItems(); // один раз для проверки, один раз для очистки
        verify(orderMapper, times(1)).toOrderDto(savedOrder);
    }

    @Test
    void createNewOrder_emptyCart_shouldThrowException() {
        when(cartStorage.getItems()).thenReturn(Collections.emptyMap());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            orderService.createNewOrder();
        });

        assertEquals("Cart is empty", ex.getMessage());
        verify(cartStorage, times(1)).getItems();
        verifyNoInteractions(itemRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void createNewOrder_itemNotFound_shouldThrowException() {
        Map<Long, Integer> cartItems = new HashMap<>();
        cartItems.put(1L, 1);

        when(cartStorage.getItems()).thenReturn(cartItems);
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> {
            orderService.createNewOrder();
        });

        assertTrue(ex.getMessage().contains("Item not found"));
        verify(itemRepository, times(1)).findById(1L);
    }
}
