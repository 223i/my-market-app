package com.iron.mymarket.dao.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("cart_items")
public class CartItem {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("item_id")
    private Long itemId;
    private Integer quantity;

    @PersistenceCreator
    public CartItem(Long userId, Long itemId, Integer quantity) {
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
