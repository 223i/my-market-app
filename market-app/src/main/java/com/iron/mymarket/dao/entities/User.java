package com.iron.mymarket.dao.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
@Getter
@Setter
public class User {

    @Id
    private Long id;            // Внутренний ID
    @Column("external_id")
    private String externalId;  // 'sub' из Keycloak
    private String email;
    private String username;

    public User(String externalId, String email, String name) {
        this.externalId = externalId;
        this.email = email;
        this.username = name;
    }
}
