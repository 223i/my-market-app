package com.iron.mymarket.util;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.entities.OrderItem;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.OrderItemDto;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {
    public ItemDto toItemDto(Item item) {
        ItemDto itemDto = new ItemDto();
        itemDto.setId(item.getId());
        itemDto.setDescription(item.getDescription());
        itemDto.setTitle(item.getTitle());
        itemDto.setImgPath(item.getImgPath());
        itemDto.setPrice(item.getPrice());
        itemDto.setCount(item.getCount());
        return itemDto;
    }


    public OrderItemDto toOrderItemDto(OrderItem orderItem, Item item) {
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItem(toItemDto(item));
        orderItemDto.setQuantity(orderItem.getQuantity());
        orderItemDto.setPriceAtPurchase(orderItem.getPriceAtPurchase());
        return orderItemDto;
    }
}
