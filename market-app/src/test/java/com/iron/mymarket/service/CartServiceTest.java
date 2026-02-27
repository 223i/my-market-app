package com.iron.mymarket.service;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.repository.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.model.ItemAction;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.util.ItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CartServiceTest {

    private ItemRepository itemRepository;
    private CartStorage cartStorage;
    private ItemMapper itemMapper;
    private CacheService cacheService;

    private CartService cartService;

    private Item item1;
    private Item item2;

    private ItemDto itemDto1;
    private ItemDto itemDto2;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        cartStorage = mock(CartStorage.class);
        itemMapper = mock(ItemMapper.class);
        cacheService = mock(CacheService.class);

        cartService = new CartService(itemRepository, itemMapper, cacheService);

        MockitoAnnotations.openMocks(this);

        // Создаём реальные объекты Item с id
        item1 = new Item();
        item1.setId(1L);
        item1.setTitle("item1");
        item1.setPrice(100);

        item2 = new Item();
        item2.setId(2L);
        item2.setTitle("item2");
        item2.setPrice(200);

        // Создаём соответствующие DTO
        itemDto1 = new ItemDto(1L, "item1", "", "", 100, 2);
        itemDto2 = new ItemDto(2L, "item2", "", "", 200, 1);

    }

    @Test
    void getItemView_shouldReturnItemDtoWithCount() {
        cartStorage = mock(CartStorage.class);
        Item item = new Item();
        item.setId(1L);
        item.setTitle("Test item");
        ItemDto dto = new ItemDto(1L, "Test item", "", "", 100, 0);

        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cacheService.get("items:1")).thenReturn(Mono.empty());
        when(cacheService.setWithExpiration("items:1", dto,  Duration.ofMinutes(5))).thenReturn(Mono.just(true));
        when(itemMapper.toItemDto(item)).thenReturn(dto);
        when(cartStorage.getCount(1L)).thenReturn(3);

        Mono<ItemDto> result = cartService.getItemView(1L, cartStorage);

        StepVerifier.create(result)
                .expectNextMatches(itemResult -> itemResult.getTitle().equals("Test item")
                        && itemResult.getId() == 1L
                        && itemResult.getCount() == 3)
                .expectComplete()
                .verify();

        verify(itemRepository).findById(1L);
        verify(cartStorage).getCount(1L);
        verify(itemMapper).toItemDto(item);
    }

    @Test
    void getItemView_shouldThrowException_whenItemNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Mono.empty());
        when(cacheService.get("items:" + 99L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> cartService.getItemView(99L, cartStorage).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item not found");
    }

    @Test
    void getCartItems_shouldReturnAllItemsFromCart() {
        Map<Long, Integer> cartItems = new HashMap<>();
        cartItems.put(1L, 2); // 2 шт. item1
        cartItems.put(2L, 1); // 1 шт. item2

        when(cartStorage.getItems()).thenReturn(cartItems);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item2));
        when(cartStorage.getCount(1L)).thenReturn(2);
        when(cartStorage.getCount(2L)).thenReturn(1);
        when(cacheService.get("items:" + 1)).thenReturn(Mono.just((Object) itemDto1));
        when(cacheService.get("items:" + 2)).thenReturn(Mono.just((Object) itemDto2));

        when(itemMapper.toItemDto(item1)).thenReturn(new ItemDto(1L, "item1", "", "", 100, 2));
        when(itemMapper.toItemDto(item2)).thenReturn(new ItemDto(2L, "item2", "", "", 200, 1));

        List<ItemDto> result = cartService.getCartItems(cartStorage).collectList().block();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2, result.get(0).getCount());
        assertEquals(2L, result.get(1).getId());
        assertEquals(1, result.get(1).getCount());

        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).findById(2L);
        verify(cartStorage, times(1)).getItems();
    }

    @Test
    void getTotal_shouldReturnCorrectSum() {
        Map<Long, Integer> cartItems = new HashMap<>();
        cartItems.put(1L, 2); // 2 шт. по 100
        cartItems.put(2L, 1); // 1 шт. по 200

        when(cartStorage.getItems()).thenReturn(cartItems);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(2L)).thenReturn(Mono.just(item2));
        when(cartStorage.getCount(1L)).thenReturn(2);
        when(cartStorage.getCount(2L)).thenReturn(1);

        when(itemMapper.toItemDto(item1)).thenReturn(
                new ItemDto(1L, "item1", "", "", 100, 2));
        when(cacheService.get("items:1")).thenReturn(Mono.just(
                new ItemDto(1L, "item1", "", "", 100, 2)));
        when(itemMapper.toItemDto(item2)).thenReturn(
                new ItemDto(2L, "item2", "", "", 200, 1));
        when(cacheService.get("items:2")).thenReturn(Mono.just(
                new ItemDto(2L, "item2", "", "", 200, 1)));

        long total = cartService.getTotal(cartStorage).block();
        // 2*100 + 1*200 = 400
        assertEquals(400, total);
    }

    @Test
    void changeItemCount_shouldCallPlusMinusDelete() {
        Item item = new Item();
        item.setId(1L);
        when(itemRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));
        when(cacheService.delete("items:1")).thenReturn(Mono.just(true));


        cartService.changeItemCount(1L, ItemAction.PLUS, cartStorage).block();
        verify(itemRepository).findById(1L);
        verify(cartStorage).plus(1L);


        cartService.changeItemCount(1L, ItemAction.MINUS, cartStorage).block();
        verify(cartStorage).minus(1L);

        cartService.changeItemCount(1L, ItemAction.DELETE, cartStorage).block();
        verify(cartStorage).delete(1L);
    }

    @Test
    void changeItemCount_shouldThrowException_whenItemNotExists() {
        when(itemRepository.findById(99L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> cartService.changeItemCount(99L, ItemAction.PLUS, cartStorage).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item not found");
    }
}

