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

import java.time.Duration;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CacheService cacheService;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);


    public ItemService(ItemRepository itemRepository, ItemMapper itemMapper, CacheService cacheService) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.cacheService = cacheService;
    }

    public Flux<ItemDto> findItems(String search,
                                   ItemSort sort,
                                   int pageNumber,
                                   int pageSize) {

        String cacheKey = String.format("items:search:%s:sort:%s:page:%d:size:%d",
                search, sort, pageNumber, pageSize);

        return cacheService.get(cacheKey)
                .cast(ItemDto[].class)
                .flatMapMany(Flux::fromArray)
                .switchIfEmpty(
                        fetchItemsFromDatabase(search, sort, pageNumber, pageSize)
                                .collectList()
                                .flatMap(items -> cacheService.setWithExpiration(cacheKey, items, CACHE_TTL)
                                        .thenReturn(items))
                                .flatMapMany(Flux::fromIterable)
                );
    }


    public Mono<ItemDto> getItemById(Long id) {
        String cacheKey = String.format("item:id:%d", id);

        return cacheService.get(cacheKey)
                .cast(ItemDto.class)
                .flatMap(Mono::just)
                .switchIfEmpty(itemRepository.findById(id)
                        .flatMap(item -> cacheService.setWithExpiration(cacheKey, item, CACHE_TTL)
                                .thenReturn(item))
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found: " + id)))
                        .map(itemMapper::toItemDto));
    }

    private Flux<ItemDto> fetchItemsFromDatabase(String search, ItemSort sort, int pageNumber, int pageSize) {
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

}
