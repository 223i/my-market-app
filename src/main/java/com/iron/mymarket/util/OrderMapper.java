package com.iron.mymarket.util;

import com.iron.mymarket.dao.entities.Order;
import com.iron.mymarket.dao.entities.OrderItem;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.OrderDto;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private ItemMapper itemMapper;

    public OrderDto toOrderDto(Order order) {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());
        orderDto.setTotalSum(order.getTotalSum());
        return orderDto;
    }

    private ItemDto fromOrderItemToItemDto(OrderItem item){
        ItemDto itemDto = new ItemDto();
        itemDto.setTitle(item.getItem().getTitle());
        itemDto.setPrice(item.getSubtotal());
        itemDto.setCount(item.getQuantity());
        return itemDto;
    }
}
