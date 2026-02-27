package com.iron.mymarket.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @BeforeEach
    void setUp() {
        // Очищаем Redis перед каждым тестом
        reactiveRedisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .serverCommands()
                .flushAll()
                .block();
    }

    @AfterEach
    void tearDown() {
        // Очищаем Redis после каждого теста
        reactiveRedisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .serverCommands()
                .flushAll()
                .block();
    }

    @Test
    void set_shouldStoreValueInRedis() {
        String key = "test:key";
        String value = "test value";

        Mono<Boolean> result = cacheService.set(key, value);

        StepVerifier.create(result)
                .expectNext(true)
                .expectComplete()
                .verify();

        // Проверяем, что значение действительно сохранено
        Mono<Object> storedValue = cacheService.get(key);
        StepVerifier.create(storedValue)
                .expectNext(value)
                .expectComplete()
                .verify();
    }

    @Test
    void setWithExpiration_shouldStoreValueWithTTL() {
        String key = "test:key:ttl";
        String value = "test value with ttl";
        Duration ttl = Duration.ofSeconds(1);

        Mono<Boolean> result = cacheService.setWithExpiration(key, value, ttl);

        StepVerifier.create(result)
                .expectNext(true)
                .expectComplete()
                .verify();

        // Проверяем, что значение есть сразу после сохранения
        Mono<Object> storedValue = cacheService.get(key);
        StepVerifier.create(storedValue)
                .expectNext(value)
                .expectComplete()
                .verify();

        // Ждем истечения TTL и проверяем, что значение исчезло
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Mono<Object> expiredValue = cacheService.get(key);
        StepVerifier.create(expiredValue)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void get_shouldReturnStoredValue() {
        String key = "test:get";
        String value = "test get value";

        // Сначала сохраняем значение напрямую через RedisTemplate
        reactiveRedisTemplate.opsForValue()
                .set(key, value)
                .block();

        Mono<Object> result = cacheService.get(key);

        StepVerifier.create(result)
                .expectNext(value)
                .expectComplete()
                .verify();
    }

    @Test
    void get_shouldReturnEmptyForNonExistentKey() {
        String key = "test:nonexistent";

        Mono<Object> result = cacheService.get(key);

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void delete_shouldRemoveKey() {
        String key = "test:delete";
        String value = "test value to delete";

        // Сохраняем значение
        cacheService.set(key, value).block();

        // Проверяем, что значение есть
        Mono<Object> beforeDelete = cacheService.get(key);
        StepVerifier.create(beforeDelete)
                .expectNext(value)
                .expectComplete()
                .verify();

        // Удаляем
        Mono<Boolean> deleteResult = cacheService.delete(key);
        StepVerifier.create(deleteResult)
                .expectNext(true)
                .expectComplete()
                .verify();

        // Проверяем, что значение удалено
        Mono<Object> afterDelete = cacheService.get(key);
        StepVerifier.create(afterDelete)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void delete_shouldReturnFalseForNonExistentKey() {
        String key = "test:delete:nonexistent";

        Mono<Boolean> result = cacheService.delete(key);

        StepVerifier.create(result)
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    void exists_shouldReturnTrueForExistingKey() {
        String key = "test:exists";
        String value = "test value";

        cacheService.set(key, value).block();

        Mono<Boolean> result = cacheService.exists(key);

        StepVerifier.create(result)
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    void exists_shouldReturnFalseForNonExistentKey() {
        String key = "test:exists:nonexistent";

        Mono<Boolean> result = cacheService.exists(key);

        StepVerifier.create(result)
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    void generateKey_shouldCreateKeyWithParts() {
        String prefix = "test";
        String part1 = "part1";
        String part2 = "part2";

        Mono<String> result = cacheService.generateKey(prefix, part1, part2);

        StepVerifier.create(result)
                .expectNext("part1:part2") // Метод объединяет только части, префикс добавляется только если есть части
                .expectComplete()
                .verify();
    }

    @Test
    void deleteByPattern_shouldRemoveKeysMatchingPattern() {
        // Сохраняем несколько ключей
        cacheService.set("test:pattern:1", "value1").block();
        cacheService.set("test:pattern:2", "value2").block();
        cacheService.set("test:other:3", "value3").block();
        cacheService.set("other:pattern:4", "value4").block();

        // Удаляем по шаблону
        Mono<Long> result = cacheService.deleteByPattern("test:pattern:*");

        StepVerifier.create(result)
                .expectNext(2L) // Должно удалиться 2 ключа
                .expectComplete()
                .verify();

        // Проверяем, что ключи с шаблоном удалены
        StepVerifier.create(cacheService.get("test:pattern:1"))
                .expectNextCount(0)
                .verifyComplete();

        StepVerifier.create(cacheService.get("test:pattern:2"))
                .expectNextCount(0)
                .verifyComplete();

        // Проверяем, что другие ключи остались
        StepVerifier.create(cacheService.get("test:other:3"))
                .expectNext("value3")
                .expectComplete()
                .verify();

        StepVerifier.create(cacheService.get("other:pattern:4"))
                .expectNext("value4")
                .expectComplete()
                .verify();
    }

    @Test
    void complexObjectSerialization_shouldWorkCorrectly() {
        String key = "test:complex";
        TestObject testObject = new TestObject("test name", 123, List.of("item1", "item2"));

        Mono<Boolean> setResult = cacheService.set(key, testObject);

        StepVerifier.create(setResult)
                .expectNext(true)
                .expectComplete()
                .verify();

        Mono<Object> getResult = cacheService.get(key);

        StepVerifier.create(getResult)
                .expectNextMatches(obj -> {
                    if (obj instanceof TestObject) {
                        TestObject retrieved = (TestObject) obj;
                        return retrieved.getName().equals(testObject.getName()) &&
                               retrieved.getValue() == testObject.getValue() &&
                               retrieved.getItems().equals(testObject.getItems());
                    }
                    return false;
                })
                .expectComplete()
                .verify();
    }

    // Вспомогательный класс для теста сериализации
    public static class TestObject {
        private String name;
        private int value;
        private List<String> items;

        public TestObject() {}

        public TestObject(String name, int value, List<String> items) {
            this.name = name;
            this.value = value;
            this.items = items;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }

        public List<String> getItems() { return items; }
        public void setItems(List<String> items) { this.items = items; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return value == that.value &&
                   java.util.Objects.equals(name, that.name) &&
                   java.util.Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, value, items);
        }
    }
}
