package com.iron.mymarket.model;

public record Paging (
    int pageSize,
    int pageNumber,
    boolean hasPrevious,
    boolean hasNext) {
}
