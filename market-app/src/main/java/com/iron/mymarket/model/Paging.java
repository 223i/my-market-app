package com.iron.mymarket.model;

public record Paging (
    int pageSize,
    int pageNumber,
    boolean hasPrevious,
    boolean hasNext) {

    public int getPageSize() { return pageSize; }
    public int getPageNumber() { return pageNumber; }
    public boolean isHasPrevious() { return hasPrevious; } // boolean лучше is*
    public boolean isHasNext() { return hasNext; }
}
