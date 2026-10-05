-- =====================================================================
-- 04_triggers.sql
-- Триггеры — глава 3.6 "Реализация ограничений, автоматизация
-- обработки данных в БД". Методичка требует не менее трёх; ниже — 4.
--
-- Важно: эти триггеры дублируют часть логики, которая в приложении
-- уже реализована в Java (OrderProcessor, DeliveryService). Это
-- сделано намеренно — они выступают как страховочный слой целостности
-- данных на уровне СУБД: правило будет соблюдено, даже если запись
-- в базу придёт в обход приложения (другой сервис, ручной SQL,
-- миграция данных и т.п.).
-- =====================================================================

-- ---------------------------------------------------------------------
-- ТРИГГЕР 1. Автоматическое ведение истории статусов заказа.
-- При изменении orders.order_status автоматически добавляется запись
-- в order_status_history — гарантирует, что история не может
-- "рассинхронизироваться" с текущим статусом заказа.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_fn_log_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.order_status IS DISTINCT FROM OLD.order_status THEN
        INSERT INTO order_status_history (order_id, status, changed_at)
        VALUES (NEW.id, NEW.order_status, now());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_order_status_history ON orders;
CREATE TRIGGER trg_order_status_history
    AFTER UPDATE OF order_status ON orders
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_log_order_status_change();


-- ---------------------------------------------------------------------
-- ТРИГГЕР 2. Проверка целостности позиции заказа при добавлении.
-- Товар должен быть доступен (available = TRUE) и принадлежать тому
-- же ресторану, что и заказ. Повторяет бизнес-правило из
-- OrderProcessor.create(...), но уже на уровне БД.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_fn_check_order_item()
RETURNS TRIGGER AS $$
DECLARE
    v_product_available BOOLEAN;
    v_product_restaurant BIGINT;
    v_order_restaurant   BIGINT;
BEGIN
    SELECT available, restaurant_id INTO v_product_available, v_product_restaurant
    FROM products WHERE id = NEW.product_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Товар с id=% не найден', NEW.product_id;
    END IF;

    IF NOT v_product_available THEN
        RAISE EXCEPTION 'Товар с id=% недоступен для заказа', NEW.product_id;
    END IF;

    SELECT restaurant_id INTO v_order_restaurant
    FROM orders WHERE id = NEW.order_id;

    IF v_order_restaurant IS DISTINCT FROM v_product_restaurant THEN
        RAISE EXCEPTION
            'Товар id=% принадлежит ресторану id=%, а заказ id=% оформлен на ресторан id=%',
            NEW.product_id, v_product_restaurant, NEW.order_id, v_order_restaurant;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_order_item ON order_item_entity;
CREATE TRIGGER trg_check_order_item
    BEFORE INSERT ON order_item_entity
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_check_order_item();


-- ---------------------------------------------------------------------
-- ТРИГГЕР 3. Автоматическое "занятие" курьера при назначении доставки.
-- При появлении записи в delivery_entity курьер сразу помечается
-- как недоступный (available = FALSE), чтобы диспетчер не назначил
-- его на второй заказ параллельно.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_fn_mark_courier_busy()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE couriers SET available = FALSE WHERE id = NEW.courier_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_courier_busy_on_delivery ON delivery_entity;
CREATE TRIGGER trg_courier_busy_on_delivery
    AFTER INSERT ON delivery_entity
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_mark_courier_busy();


-- ---------------------------------------------------------------------
-- ТРИГГЕР 4. Автоматическое освобождение курьера при доставке заказа.
-- Как только заказ переходит в статус DELIVERED, курьер, закреплённый
-- за этим заказом, снова становится доступным (available = TRUE).
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trg_fn_free_courier_on_delivered()
RETURNS TRIGGER AS $$
DECLARE
    v_courier_id BIGINT;
BEGIN
    IF NEW.order_status = 'DELIVERED'
       AND OLD.order_status IS DISTINCT FROM 'DELIVERED' THEN

        SELECT courier_id INTO v_courier_id
        FROM delivery_entity WHERE order_id = NEW.id;

        IF FOUND THEN
            UPDATE couriers SET available = TRUE WHERE id = v_courier_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_free_courier_on_delivered ON orders;
CREATE TRIGGER trg_free_courier_on_delivered
    AFTER UPDATE OF order_status ON orders
    FOR EACH ROW
    EXECUTE FUNCTION trg_fn_free_courier_on_delivered();
