-- =====================================================================
-- 02_views.sql
-- Представления (VIEW) — глава 3.3 "Разработка представлений"
-- =====================================================================

-- ---------------------------------------------------------------------
-- v_order_summary
-- Сводная информация по заказу: покупатель, ресторан, адрес,
-- количество позиций, сумма. Используется для отчёта "Список заказов".
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_order_summary AS
SELECT
    o.id                                        AS order_id,
    c.name                                      AS customer_name,
    c.phone                                     AS customer_phone,
    r.name                                      AS restaurant_name,
    a.city || ', ' || a.street || ' ' || a.house
        || COALESCE(', кв. ' || a.apartment, '') AS delivery_address,
    o.order_status,
    o.total_amount,
    COUNT(oi.id)                                AS items_count
FROM orders o
JOIN customers c   ON c.id = o.customer_id
JOIN restaurants r ON r.id = o.restaurant_id
JOIN addresses a   ON a.id = o.address_id
LEFT JOIN order_item_entity oi ON oi.order_id = o.id
GROUP BY o.id, c.name, c.phone, r.name, a.city, a.street, a.house,
         a.apartment, o.order_status, o.total_amount;

-- ---------------------------------------------------------------------
-- v_restaurant_menu
-- Действующее меню ресторана (только доступные позиции) с названием
-- категории — то, что видит покупатель в приложении.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_restaurant_menu AS
SELECT
    p.id            AS product_id,
    r.id            AS restaurant_id,
    r.name          AS restaurant_name,
    cat.name        AS category_name,
    p.name          AS product_name,
    p.description,
    p.price
FROM products p
JOIN restaurants r  ON r.id = p.restaurant_id
JOIN categories cat ON cat.id = p.category_id
WHERE p.available = TRUE;

-- ---------------------------------------------------------------------
-- v_courier_workload
-- Текущая загрузка курьеров: сколько активных (незавершённых) доставок
-- на каждом курьере прямо сейчас. Помогает диспетчеру видеть нагрузку.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_courier_workload AS
SELECT
    co.id                                   AS courier_id,
    co.name                                 AS courier_name,
    co.phone,
    co.available,
    COUNT(d.id) FILTER (
        WHERE o.order_status = 'DELIVERY_ASSIGNED'
    )                                       AS active_deliveries
FROM couriers co
LEFT JOIN delivery_entity d ON d.courier_id = co.id
LEFT JOIN orders o          ON o.id = d.order_id
GROUP BY co.id, co.name, co.phone, co.available;

-- ---------------------------------------------------------------------
-- v_customer_order_history
-- История заказов покупателя вместе с отзывом (если он оставлен).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_customer_order_history AS
SELECT
    c.id            AS customer_id,
    c.name          AS customer_name,
    o.id            AS order_id,
    o.order_status,
    o.total_amount,
    rv.rating,
    rv.comment      AS review_comment
FROM customers c
JOIN orders o        ON o.customer_id = c.id
LEFT JOIN reviews rv ON rv.order_id = o.id AND rv.customer_id = c.id;
