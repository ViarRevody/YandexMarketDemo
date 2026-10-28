-- Demo records for the Postman order flow. Run after 01_ddl_schema.sql.
-- The fixed IDs make the accompanying create-order request repeatable.
BEGIN;

INSERT INTO customers (id, name, email, phone)
VALUES (900001, 'Postman Demo Customer', 'postman-demo@example.com', '+79990000091')
ON CONFLICT DO NOTHING;

INSERT INTO addresses (id, customer_id, city, street, house, apartment)
VALUES (900001, 900001, 'Москва', 'Тестовая', '1', '1')
ON CONFLICT DO NOTHING;

INSERT INTO restaurants (id, name, address, phone)
VALUES (900001, 'Postman Demo Restaurant', 'Москва, Тестовая, 1', '+79990000092')
ON CONFLICT DO NOTHING;

INSERT INTO categories (id, name)
VALUES (900001, 'Postman Demo')
ON CONFLICT DO NOTHING;

INSERT INTO products (
    id, restaurant_id, category_id, name, description, price, available
)
VALUES (
    900001, 900001, 900001, 'Postman Demo Item', 'Тестовый товар', 350.00, TRUE
)
ON CONFLICT DO NOTHING;

SELECT setval('customers_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM customers), 900001, (SELECT last_value FROM customers_seq)));
SELECT setval('addresses_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM addresses), 900001, (SELECT last_value FROM addresses_seq)));
SELECT setval('restaurants_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM restaurants), 900001, (SELECT last_value FROM restaurants_seq)));
SELECT setval('categories_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM categories), 900001, (SELECT last_value FROM categories_seq)));
SELECT setval('products_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM products), 900001, (SELECT last_value FROM products_seq)));
SELECT setval('orders_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM orders), (SELECT last_value FROM orders_seq)));
SELECT setval('order_item_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM order_item_entity), (SELECT last_value FROM order_item_seq)));
SELECT setval('order_status_history_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM order_status_history), (SELECT last_value FROM order_status_history_seq)));
SELECT setval('couriers_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM couriers), (SELECT last_value FROM couriers_seq)));
SELECT setval('delivery_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM delivery_entity), (SELECT last_value FROM delivery_seq)));
SELECT setval('reviews_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM reviews), (SELECT last_value FROM reviews_seq)));
SELECT setval('payments_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM payments), (SELECT last_value FROM payments_seq)));

INSERT INTO couriers (name, phone, available)
VALUES
    ('Ахмад', '+79990000001', TRUE),
    ('Магомед', '+79990000002', TRUE),
    ('Али', '+79990000003', TRUE)
ON CONFLICT (phone) DO NOTHING;

UPDATE couriers c
SET available = TRUE
WHERE c.phone IN ('+79990000001', '+79990000002', '+79990000003')
  AND NOT EXISTS (
      SELECT 1
      FROM delivery_entity d
      JOIN orders o ON o.id = d.order_id
      WHERE d.courier_id = c.id
        AND o.order_status = 'DELIVERY_ASSIGNED'
  );

COMMIT;
