package com.iron.mymarket.dao.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table(name = "orders")
public class Order {

    @Id
    private Long id;
    private long totalSum;
}
