package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.util.ItemMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    public Page<ItemDto> findItems(String search, ItemSort sort,
                                   Integer pageNumber, Integer pageSize) {
        Sort springSort = switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, springSort);

        Page<Item> foundItems;
        if (!search.isBlank()) {
            foundItems = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search,
                    search, pageable);
        } else {
            foundItems = itemRepository.findAll(pageable);
        }
        return foundItems.map(itemMapper::toItemDto);
    }


    public ItemDto getItemById(Long id){
        Item itemById = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));
        return itemMapper.toItemDto(itemById);
    }


}
