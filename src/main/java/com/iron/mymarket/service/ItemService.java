package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.util.ItemMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    public Flux<ItemDto> findItems(String search,
                                   ItemSort sort,
                                   int pageNumber,
                                   int pageSize) {

        Sort springSort = switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, springSort);

        Flux<Item> foundItems;
        if (!search.isBlank()) {
            foundItems = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search,
                    search, pageable);
        } else {
            foundItems = itemRepository.findAllBy(pageable);
        }

        return foundItems.map(itemMapper::toItemDto);
    }


    public Mono<ItemDto> getItemById(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found: " + id)))
                .map(itemMapper::toItemDto);
    }
}
