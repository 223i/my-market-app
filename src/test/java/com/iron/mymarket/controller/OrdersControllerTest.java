package com.iron.mymarket.controller;

import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdersController.class)
public class OrdersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private OrderDto order1;
    private OrderDto order2;

    @BeforeEach
    void setUp() {
        order1 = new OrderDto(1L, List.of(), 1000);
        order2 = new OrderDto(2L, List.of(), 2500);
    }

    @Test
    void getOrders_shouldReturnOrdersPageWithOrders() throws Exception {
        when(orderService.findOrders()).thenReturn(List.of(order1, order2));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", hasSize(2)));

        verify(orderService, times(1)).findOrders();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void createNewOrder_shouldRedirectToNewOrderPage() throws Exception {
        OrderDto newOrder = new OrderDto(99L, List.of(), 5000);
        when(orderService.createNewOrder()).thenReturn(newOrder);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/99?newOrder=true"));

        verify(orderService, times(1)).createNewOrder();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void getOrderById_shouldReturnValidOrder() throws Exception {

        List<ItemDto> items = List.of(
                new ItemDto(1L, "Item 1", "Desc", "/img/1.jpg", 100, 0),
                new ItemDto(2L, "Item 2", "Desc", "/img/2.jpg", 200, 0)
        );
        OrderDto order = new OrderDto(1L, items, 300);

        when(orderService.findOrderById(1L)).thenReturn(order);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("order", sameInstance(order)));
    }
}
