package com.iron.mymarket.dao.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.HashMap;
import java.util.Map;

@Component
@SessionScope
public class CartStorage {

    private final Map<Long, Integer> items = new HashMap<>();

    public void plus(long itemId) {
        items.merge(itemId, 1, Integer::sum);
    }

    public void minus(long itemId) {
        items.computeIfPresent(itemId, (id, count) ->
                count > 1 ? count - 1 : null
        );
    }

    public void delete(long itemId) {
        items.remove(itemId);
    }

    public int getCount(long itemId) {
        return items.getOrDefault(itemId, 0);
    }

    public Map<Long, Integer> getItems() {
        return items;
    }
}
