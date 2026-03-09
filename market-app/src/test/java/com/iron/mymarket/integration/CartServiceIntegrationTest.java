package com.iron.mymarket.integration;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.session.CartStorage;
import com.iron.mymarket.dao.repository.ItemRepository;
import com.iron.mymarket.model.ItemDto;
import com.iron.mymarket.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @MockitoBean
    private ItemRepository itemRepository;

    private CartStorage testCart;

    @BeforeEach
    void setUp() {
        testCart = new CartStorage();
    }

    @Test
    void cartOperations_shouldWorkCorrectly() {
        // Создаем тестовые товары с разными ID, чтобы избежать конфликтов кэша
        Item item1 = new Item();
        item1.setId(100L);
        item1.setTitle("Smartphone X1");
        item1.setDescription("Modern smartphone");
        item1.setImgPath("smartphone_x1.jpeg");
        item1.setPrice(69900L);
        item1.setCount(15);

        Item item2 = new Item();
        item2.setId(200L);
        item2.setTitle("Laptop Pro 15");
        item2.setDescription("Powerful laptop");
        item2.setImgPath("laptop_pro_15.jpg");
        item2.setPrice(129900L);
        item2.setCount(7);

        // Мокируем репозиторий товаров
        when(itemRepository.findById(100L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(200L)).thenReturn(Mono.just(item2));

        // Наполняем корзину
        testCart.plus(100L); // Добавляем 2 смартфона
        testCart.plus(100L);
        testCart.plus(200L); // Добавляем 1 ноутбук

        // Проверяем содержимое корзины
        List<ItemDto> cartItems = cartService.getCartItems(testCart).collectList().block();
        assert cartItems != null;
        assert cartItems.size() == 2 : "Expected 2 items, got " + cartItems.size();
        assert cartItems.stream().anyMatch(item -> item.getTitle().equals("Smartphone X1")) : "Smartphone X1 not found";
        assert cartItems.stream().anyMatch(item -> item.getTitle().equals("Laptop Pro 15")) : "Laptop Pro 15 not found";

        // Проверяем количество товаров в корзине (через CartStorage)
        assert testCart.getCount(100L) == 2; // 2 смартфона
        assert testCart.getCount(200L) == 1; // 1 ноутбук

        // Проверяем общую сумму
        Long total = cartService.getTotal(testCart).block();
        assert total != null;
        assert total == 269700L; // 2*69900 + 1*129900

        // Уменьшаем количество товара
        cartService.changeItemCount(100L, com.iron.mymarket.model.ItemAction.MINUS, testCart).block();
        assert testCart.getCount(100L) == 1; // Остался 1 смартфон

        //Удаляем товар
        cartService.changeItemCount(200L, com.iron.mymarket.model.ItemAction.DELETE, testCart).block();
        assert !testCart.getItems().containsKey(200L); // Ноутбук удален

        //Проверяем обновленную сумму
        Long updatedTotal = cartService.getTotal(testCart).block();
        assert updatedTotal != null;
        assert updatedTotal == 69900L; // Только 1 смартфон

        //Очищаем корзину
        testCart.getItems().clear();
        assert testCart.getItems().isEmpty();
    }

    @Test
    void emptyCart_shouldReturnZeroTotal() {
        // Проверяем, что пустая корзина возвращает сумму 0
        Long total = cartService.getTotal(testCart).block();
        assert total != null;
        assert total == 0L;

        // Проверяем, что пустая корзина возвращает пустой список товаров
        List<ItemDto> cartItems = cartService.getCartItems(testCart).collectList().block();
        assert cartItems != null;
        assert cartItems.isEmpty();
    }

    @Test
    void cartWithNonExistentItem_shouldHandleGracefully() {
        // Добавляем товар с несуществующим ID
        testCart.plus(999L);

        //Мокируем, что товар не найден
        when(itemRepository.findById(999L)).thenReturn(Mono.empty());

        // Проверяем, что сервис обрабатывает это корректно
        try {
            cartService.getCartItems(testCart).collectList().block();
            assert false : "Ожидалось исключение о несуществующем товаре";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("Item not found:999");
        }
    }

    @Test
    void cartModificationOperations_shouldUpdateCorrectly() {
        // Создаем тестовый товар
        Item item = new Item();
        item.setId(400L);
        item.setTitle("Test Item");
        item.setDescription("Test description");
        item.setImgPath("test.jpg");
        item.setPrice(10000L);
        item.setCount(10);

        // Мокируем репозиторий
        when(itemRepository.findById(400L)).thenReturn(Mono.just(item));

        // Добавляем товар в корзину
        testCart.plus(400L);
        testCart.plus(400L);
        testCart.plus(400L);

        // Проверяем начальное количество
        assert testCart.getCount(400L) == 3;

        //Уменьшаем количество
        cartService.changeItemCount(400L, com.iron.mymarket.model.ItemAction.MINUS, testCart).block();
        assert testCart.getCount(400L) == 2;

        // Увеличиваем количество
        cartService.changeItemCount(400L, com.iron.mymarket.model.ItemAction.PLUS, testCart).block();
        assert testCart.getCount(400L) == 3;

        //Удаляем товар
        cartService.changeItemCount(400L, com.iron.mymarket.model.ItemAction.DELETE, testCart).block();
        assert testCart.getItems().isEmpty();
    }

    @Test
    void multipleItemsCartTotalCalculation_shouldBeCorrect() {
        // Создаем тестовые товары
        Item item1 = new Item();
        item1.setId(500L);
        item1.setTitle("Item 1");
        item1.setDescription("Description 1");
        item1.setImgPath("item1.jpg");
        item1.setPrice(10000L);
        item1.setCount(10);

        Item item2 = new Item();
        item2.setId(600L);
        item2.setTitle("Item 2");
        item2.setDescription("Description 2");
        item2.setImgPath("item2.jpg");
        item2.setPrice(20000L);
        item2.setCount(10);

        // Мокируем репозиторий
        when(itemRepository.findById(500L)).thenReturn(Mono.just(item1));
        when(itemRepository.findById(600L)).thenReturn(Mono.just(item2));

        //Наполняем корзину разными товарами
        testCart.plus(500L); // 1 товар по 10000
        testCart.plus(500L); // еще 1 товар по 10000
        testCart.plus(600L); // 1 товар по 20000

        //Проверяем общую сумму
        Long total = cartService.getTotal(testCart).block();
        assert total != null;
        assert total == 40000L : "Expected 40000, got " + total;

        //Проверяем содержимое
        List<ItemDto> cartItems = cartService.getCartItems(testCart).collectList().block();
        assert cartItems != null;
        assert cartItems.size() == 2;

        // Проверяем количество каждого товара через CartStorage
        assert testCart.getCount(500L) == 2; // 2 товара по 10000
        assert testCart.getCount(600L) == 1; // 1 товар по 20000
    }
}
