package com.iron.mymarket.dao.entities;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    private Long id;
    private Long orderId;
    private Long itemId;
    private int quantity;
    private long priceAtPurchase;

    @Transient
    public long getSubtotal() {
        return quantity * priceAtPurchase;
    }
}
