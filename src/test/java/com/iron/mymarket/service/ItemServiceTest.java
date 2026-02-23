package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.model.ItemSort;
import com.iron.mymarket.util.ItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getItemById_existingItem_shouldReturnDto() {
        Item item = new Item();
        item.setId(1L);
        ItemDto dto = new ItemDto();
        dto.setId(1L);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(itemMapper.toItemDto(item)).thenReturn(dto);

        ItemDto result = itemService.getItemById(1L).block();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(itemRepository, times(1)).findById(1L);
        verify(itemMapper, times(1)).toItemDto(item);
    }

    @Test
    void getItemById_nonExistingItem_shouldThrowException() {
        when(itemRepository.findById(1L)).thenReturn(Mono.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            itemService.getItemById(1L).block();
        });

        assertTrue(ex.getMessage().contains("Item not found"));
        verify(itemRepository, times(1)).findById(1L);
        verifyNoInteractions(itemMapper);
    }

    @Test
    void findItems_noSearch_shouldReturnAllItems() {
        Item item1 = new Item();
        item1.setId(1L);
        item1.setTitle("A");
        item1.setPrice(100);
        Item item2 = new Item();
        item2.setId(2L);
        item2.setTitle("B");
        item2.setPrice(200);

        Flux<Item> itemsFlux = Flux.just(item1, item2);

        when(itemRepository.findAllBy(PageRequest.of(0, 5, Sort.unsorted()))).thenReturn(itemsFlux);
        when(itemMapper.toItemDto(item1)).thenReturn(new ItemDto(1L, "A", "", "", 100, 0));
        when(itemMapper.toItemDto(item2)).thenReturn(new ItemDto(2L, "B", "", "", 200, 0));

        List<ItemDto> result = itemService.findItems("", ItemSort.NO, 1, 5).collectList().block();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getTitle());
        assertEquals("B", result.get(1).getTitle());

        verify(itemRepository, times(1)).findAllBy(any());
    }

    @Test
    void findItems_withSearch_shouldReturnFilteredItems() {
        String search = "ball";
        Item item = new Item();
        item.setId(1L);
        item.setTitle("Ball");
        item.setDescription("Round");
        Flux<Item> page = Flux.just(item);

        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search, search, PageRequest.of(0, 5, Sort.unsorted()))).thenReturn(page);
        when(itemMapper.toItemDto(item)).thenReturn(new ItemDto(1L, "Ball", "Round", "", 100, 0));

        List<ItemDto> result = itemService.findItems(search, ItemSort.NO, 1, 5).collectList().block();

        assertEquals(1, result.size());
        assertEquals("Ball", result.get(0).getTitle());

        verify(itemRepository, times(1))
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search,
                        PageRequest.of(0, 5, Sort.unsorted()));
    }
}
