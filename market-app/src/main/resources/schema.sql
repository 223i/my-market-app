-- ITEMS
CREATE TABLE items
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description CLOB,
    img_path    VARCHAR(255),
    price       BIGINT       NOT NULL,
    count       INT          NOT NULL
);

-- ORDERS
CREATE TABLE orders
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_sum BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(external_id);
);

-- ORDER_ITEMS
CREATE TABLE order_items
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT NOT NULL,
    item_id           BIGINT NOT NULL,
    quantity          INT    NOT NULL,
    price_at_purchase BIGINT NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_order_items_item
        FOREIGN KEY (item_id)
            REFERENCES items (id)
);

-- USERS
CREATE TABLE IF NOT EXISTS users (
    -- id из Keycloak
    external_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- USER CART
CREATE TABLE IF NOT EXISTS cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(external_id),
    cart_data CLOB
);