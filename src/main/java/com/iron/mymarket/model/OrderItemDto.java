package com.iron.mymarket.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrderItemDto {
    private ItemDto item;
    private int quantity;
    private long priceAtPurchase;

    public long getSubtotal() {
        return quantity * priceAtPurchase;
    }
}
