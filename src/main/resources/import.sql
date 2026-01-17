INSERT INTO items (title, description, img_path, price, count) VALUES ('Smartphone X1', 'Modern smartphone with 128GB storage and OLED display', '/img/products/smartphone_x1.jpg', 69900, 15);

INSERT INTO items (title, description, img_path, price, count) VALUES ('Laptop Pro 15', 'Powerful laptop for work and development, 16GB RAM, SSD', '/img/products/laptop_pro_15.jpg', 129900, 7);

INSERT INTO items (title, description, img_path, price, count) VALUES ('Wireless Headphones', 'Noise-cancelling wireless headphones with long battery life', '/img/products/wireless_headphones.jpg', 19900, 25);

INSERT INTO items (title, description, img_path, price, count) VALUES ('Smart Watch S', 'Smart watch with heart rate monitoring and GPS', '/img/products/smart_watch_s.jpg', 24900, 12);

INSERT INTO items (title, description, img_path, price, count) VALUES ('Bluetooth Speaker', 'Portable bluetooth speaker with deep bass and water resistance', '/img/products/bluetooth_speaker.jpg', 8900, 30);

INSERT INTO items (title, description, img_path, price, count) VALUES ('Gaming Mouse', 'Ergonomic gaming mouse with adjustable DPI and RGB lighting', '/img/products/gaming_mouse.jpg', 5900, 40);

INSERT INTO items (title, description, img_path, price, count) VALUES ('Mechanical Keyboard', 'Mechanical keyboard with blue switches and backlight', '/img/products/mechanical_keyboard.jpg', 10900, 20);

INSERT INTO items (title, description, img_path, price, count) VALUES ('4K Monitor 27', '27-inch 4K UHD monitor for professional work', '/img/products/4k_monitor_27.jpg', 45900, 9);

INSERT INTO items (title, description, img_path, price, count) VALUES ('External SSD 1TB', 'High-speed external SSD with USB-C support', '/img/products/external_ssd_1tb.jpg', 17900, 18);

INSERT INTO items (title, description, img_path, price, count) VALUES ('USB-C Hub', 'Multiport USB-C hub with HDMI and Ethernet', '/img/products/usb_c_hub.jpg', 4900, 35);



INSERT INTO orders (id, total_sum) VALUES (1, 89700);
INSERT INTO orders (id, total_sum) VALUES (2, 135800);
INSERT INTO orders (id, total_sum) VALUES (3, 24900);
INSERT INTO orders (id, total_sum) VALUES (4, 54800);
INSERT INTO orders (id, total_sum) VALUES (5, 184600);

-- Order #1 Smartphone X1 (1 × 69900) Gaming Mouse (1 × 5900) USB-C Hub (3 × 4900)

INSERT INTO order_items (order_id, item_id, quantity, price)
VALUES
(1, 1, 1, 69900),
(1, 6, 1, 5900),
(1, 10, 3, 4900);

-- Order #2 Laptop Pro 15 (1 × 129900) USB-C Hub (1 × 4900) Wireless Headphones (1 × 19900)

INSERT INTO order_items (order_id, item_id, quantity, price)
VALUES
(2, 2, 1, 129900),
(2, 10, 1, 4900),
(2, 3, 1, 19900);

-- Order #3 Smart Watch S (1 × 24900)

INSERT INTO order_items (order_id, item_id, quantity, price)
VALUES
(3, 4, 1, 24900);

-- Order #4 Mechanical Keyboard (2 × 10900) Gaming Mouse (2 × 5900) Bluetooth Speaker (1 × 8900)

INSERT INTO order_items (order_id, item_id, quantity, price)
VALUES
(4, 7, 2, 10900),
(4, 6, 2, 5900),
(4, 5, 1, 8900);

-- Order #5 4K Monitor 27 (2 × 45900) External SSD 1TB (1 × 17900) USB-C Hub (1 × 4900) Wireless Headphones (1 × 19900)

INSERT INTO order_items (order_id, item_id, quantity, price)
VALUES
(5, 8, 2, 45900),
(5, 9, 1, 17900),
(5, 10, 1, 4900),
(5, 3, 1, 19900);