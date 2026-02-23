package com.iron.mymarket.repository;

import com.iron.mymarket.dao.entities.Item;
import com.iron.mymarket.dao.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataR2dbcTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldReturnItems() {
        Item item1 = new Item();
        item1.setTitle("Ball");
        item1.setDescription("Round");
        item1.setPrice(100L);
        itemRepository.save(item1).block();

        Item item2 = new Item();
        item2.setTitle("Bat");
        item2.setDescription("Wooden");
        item2.setPrice(150L);
        itemRepository.save(item2).block();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());
        Flux<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "ball", "ball", pageable);

        StepVerifier.create(result)
                .expectNextMatches(item -> item.getTitle().equals("Ball"))
                .expectComplete()
                .verify();
    }
}
