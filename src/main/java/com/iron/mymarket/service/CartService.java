package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.util.ItemMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ItemRepository itemRepository;
    private final CartStorage cartStorage;
    private final ItemMapper itemMapper;

    public ItemDto getItemView(long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found"));
        ItemDto view = itemMapper.toItemDto(item);
        view.setCount(cartStorage.getCount(itemId));
        return view;
    }

    public List<ItemDto> getCartItems() {
        return cartStorage.getItems().keySet().stream()
                .map(this::getItemView)
                .toList();
    }

    public long getTotal() {
        return getCartItems().stream()
                .mapToLong(i -> i.getPrice() * i.getCount())
                .sum();
    }

    public void changeItemCount(long itemId, ItemAction action) {
        if (!itemRepository.existsById(itemId)) throw new EntityNotFoundException("Item not found");

        switch (action) {
            case PLUS -> cartStorage.plus(itemId);
            case MINUS -> cartStorage.minus(itemId);
        }
    }
}

