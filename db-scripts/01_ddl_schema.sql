-- =====================================================================
-- 01_ddl_schema.sql
-- Физическая (даталогическая) модель БД "Служба доставки еды"
-- (YandexMarketDB), PostgreSQL / Postgres Pro
--
-- Схема полностью соответствует JPA-сущностям проекта:
--   order-service:    customers, addresses, restaurants, categories,
--                     products, orders, order_item_entity,
--                     order_status_history
--   delivery-service: couriers, delivery_entity, reviews
--   payment-service:  payments
--
-- Скрипт можно выполнить один раз на чистой БД ДО первого запуска
-- Spring Boot приложений — Hibernate (ddl-auto=update) увидит уже
-- существующие таблицы и не будет их пересоздавать, а лишь дополнит
-- недостающими индексами/колонками при их появлении в будущем.
--
-- JPA-сущности явно сопоставляют dedicated sequence из этой схемы через
-- @SequenceGenerator с allocationSize = 50. Шаг всех sequence должен
-- совпадать с allocationSize, иначе Hibernate не запустит persistence unit.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. CUSTOMERS — покупатели
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS customers_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS customers (
    id      BIGINT PRIMARY KEY DEFAULT nextval('customers_seq'),
    name    VARCHAR(255) NOT NULL,
    email   VARCHAR(255) NOT NULL UNIQUE,
    phone   VARCHAR(32)  UNIQUE
);

ALTER SEQUENCE customers_seq OWNED BY customers.id;

-- ---------------------------------------------------------------------
-- 2. ADDRESSES — адреса доставки покупателя (1 customer -> N addresses)
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS addresses_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS addresses (
    id          BIGINT PRIMARY KEY DEFAULT nextval('addresses_seq'),
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    city        VARCHAR(120) NOT NULL,
    street      VARCHAR(200) NOT NULL,
    house       VARCHAR(20)  NOT NULL,
    apartment   VARCHAR(20),
    comment     VARCHAR(500)
);

ALTER SEQUENCE addresses_seq OWNED BY addresses.id;

-- ---------------------------------------------------------------------
-- 3. RESTAURANTS — рестораны-партнёры
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS restaurants_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS restaurants (
    id      BIGINT PRIMARY KEY DEFAULT nextval('restaurants_seq'),
    name    VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    phone   VARCHAR(32)
);

ALTER SEQUENCE restaurants_seq OWNED BY restaurants.id;

-- ---------------------------------------------------------------------
-- 4. CATEGORIES — категории блюд
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS categories_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS categories (
    id   BIGINT PRIMARY KEY DEFAULT nextval('categories_seq'),
    name VARCHAR(120) NOT NULL UNIQUE
);

ALTER SEQUENCE categories_seq OWNED BY categories.id;

-- ---------------------------------------------------------------------
-- 5. PRODUCTS — блюда/товары ресторана
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS products_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS products (
    id            BIGINT PRIMARY KEY DEFAULT nextval('products_seq'),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    category_id   BIGINT NOT NULL REFERENCES categories(id),
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(1000),
    price         NUMERIC(19,2) NOT NULL CHECK (price >= 0),
    available     BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER SEQUENCE products_seq OWNED BY products.id;

-- ---------------------------------------------------------------------
-- 6. ORDERS — заказы
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS orders_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT PRIMARY KEY DEFAULT nextval('orders_seq'),
    customer_id   BIGINT NOT NULL REFERENCES customers(id),
    address_id    BIGINT NOT NULL REFERENCES addresses(id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    total_amount  NUMERIC(19,2),
    order_status  VARCHAR(32) NOT NULL
        CHECK (order_status IN (
            'PENDING_PAYMENT', 'PAID', 'PAYMENT_FAILED',
            'DELIVERY_ASSIGNED', 'DELIVERED'
        ))
);

ALTER SEQUENCE orders_seq OWNED BY orders.id;

-- ---------------------------------------------------------------------
-- 7. ORDER_ITEM_ENTITY — состав заказа
--    Ассоциативная сущность связи М:М между ORDERS и PRODUCTS
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS order_item_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS order_item_entity (
    id                BIGINT PRIMARY KEY DEFAULT nextval('order_item_seq'),
    order_id          BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id        BIGINT NOT NULL REFERENCES products(id),
    quantity          INTEGER NOT NULL CHECK (quantity > 0),
    price_at_purchase NUMERIC(19,2) NOT NULL CHECK (price_at_purchase >= 0)
);

ALTER SEQUENCE order_item_seq OWNED BY order_item_entity.id;

-- ---------------------------------------------------------------------
-- 8. ORDER_STATUS_HISTORY — история смены статусов заказа
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS order_status_history_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS order_status_history (
    id         BIGINT PRIMARY KEY DEFAULT nextval('order_status_history_seq'),
    order_id   BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status     VARCHAR(32) NOT NULL
        CHECK (status IN (
            'PENDING_PAYMENT', 'PAID', 'PAYMENT_FAILED',
            'DELIVERY_ASSIGNED', 'DELIVERED'
        )),
    changed_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER SEQUENCE order_status_history_seq OWNED BY order_status_history.id;

-- ---------------------------------------------------------------------
-- 9. COURIERS — курьеры
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS couriers_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS couriers (
    id        BIGINT PRIMARY KEY DEFAULT nextval('couriers_seq'),
    name      VARCHAR(255) NOT NULL,
    phone     VARCHAR(32) NOT NULL UNIQUE,
    available BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER SEQUENCE couriers_seq OWNED BY couriers.id;

-- ---------------------------------------------------------------------
-- 10. DELIVERY_ENTITY — доставка, назначенная на заказ
--     (столбец "eda_minutes" — как в реальном коде DeliveryEntity;
--      это опечатка исходного проекта вместо "eta_minutes", оставлена
--      намеренно для совместимости с Hibernate ddl-auto=update)
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS delivery_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS delivery_entity (
    id          BIGINT PRIMARY KEY DEFAULT nextval('delivery_seq'),
    order_id    BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    courier_id  BIGINT NOT NULL REFERENCES couriers(id),
    eda_minutes INTEGER NOT NULL CHECK (eda_minutes > 0)
);

ALTER SEQUENCE delivery_seq OWNED BY delivery_entity.id;

-- ---------------------------------------------------------------------
-- 11. REVIEWS — отзывы покупателей о заказе
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS reviews_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT PRIMARY KEY DEFAULT nextval('reviews_seq'),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     VARCHAR(1000)
);

ALTER SEQUENCE reviews_seq OWNED BY reviews.id;

-- ---------------------------------------------------------------------
-- 12. PAYMENTS — платежи по заказам
-- ---------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS payments_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT PRIMARY KEY DEFAULT nextval('payments_seq'),
    order_id       BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    amount         NUMERIC(19,2),
    payment_status VARCHAR(32) NOT NULL
        CHECK (payment_status IN ('PAYMENT_SUCCEEDED', 'PAYMENT_FAILED', 'REFUNDED')),
    payment_method VARCHAR(32) NOT NULL
        CHECK (payment_method IN ('QR', 'CARD', 'YANDEX_SPLIT'))
);

ALTER SEQUENCE payments_seq OWNED BY payments.id;

-- =====================================================================
-- ИНДЕКСЫ
-- Ускоряют выборки по внешним ключам и по часто фильтруемым столбцам
-- (обосновывается в главе 3.2 — "Физическое проектирование БД")
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_addresses_customer_id       ON addresses(customer_id);
CREATE INDEX IF NOT EXISTS idx_products_restaurant_id      ON products(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_products_category_id        ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_available          ON products(available) WHERE available = TRUE;
CREATE INDEX IF NOT EXISTS idx_orders_customer_id          ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_id        ON orders(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_status         ON orders(order_status);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id         ON order_item_entity(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product_id       ON order_item_entity(product_id);
CREATE INDEX IF NOT EXISTS idx_order_status_history_order   ON order_status_history(order_id);
CREATE INDEX IF NOT EXISTS idx_delivery_courier_id         ON delivery_entity(courier_id);
CREATE INDEX IF NOT EXISTS idx_reviews_order_id            ON reviews(order_id);
CREATE INDEX IF NOT EXISTS idx_reviews_customer_id         ON reviews(customer_id);

COMMIT;
