package com.iron.mymarket.dao.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CartStorage {

    private final Map<Long, Integer> items = new ConcurrentHashMap<>();

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
