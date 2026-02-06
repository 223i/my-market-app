package com.iron.mymarket.util;

import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.model.OrderDto;
import com.iron.mymarket.model.OrderItemDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {


    public OrderDto toOrderDto(Order order, List<OrderItemDto> items) {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());
        orderDto.setTotalSum(order.getTotalSum());
        orderDto.setItems(items);
        return orderDto;
    }
}
