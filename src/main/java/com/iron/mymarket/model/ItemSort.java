package com.iron.mymarket.model;

import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum ItemSort {
    NO(Sort.unsorted()),
    ALPHA(Sort.by("title")),
    PRICE(Sort.by("price"));

    private final Sort sort;

    ItemSort(Sort sort) {
        this.sort = sort;
    }

}
