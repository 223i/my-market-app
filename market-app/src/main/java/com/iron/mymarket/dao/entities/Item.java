package com.iron.mymarket.dao.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table(name = "items")
public class Item {

    @Id
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private long price;
    private int count;
}
