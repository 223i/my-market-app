package com.iron.mymarket.util;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.model.ItemDto;
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
}
