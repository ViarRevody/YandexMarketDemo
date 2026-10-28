-- =====================================================================
-- 05_procedures.sql
-- Хранимые процедуры и функции PL/pgSQL — закрывают темы курса:
-- курсоры, обработка исключений, подзапросы.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ФУНКЦИЯ 1. fn_assign_courier(order_id)
-- Аналог DeliveryService.assignDelivery(...) на уровне БД: находит
-- первого свободного курьера и создаёт запись о доставке.
-- Обработка исключений: если заказ не найден или свободных курьеров
-- нет — понятная ошибка вместо "тихого" сбоя.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_assign_courier(p_order_id BIGINT)
RETURNS BIGINT AS $$
DECLARE
    v_courier_id  BIGINT;
    v_delivery_id BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM orders WHERE id = p_order_id) THEN
        RAISE EXCEPTION 'Заказ id=% не найден', p_order_id;
    END IF;

    IF EXISTS (SELECT 1 FROM delivery_entity WHERE order_id = p_order_id) THEN
        RAISE EXCEPTION 'Доставка для заказа id=% уже назначена', p_order_id;
    END IF;

    SELECT id INTO v_courier_id
    FROM couriers
    WHERE available = TRUE
    ORDER BY id
    LIMIT 1
    FOR UPDATE SKIP LOCKED;   -- защита от гонки при параллельных вызовах

    IF v_courier_id IS NULL THEN
        RAISE EXCEPTION 'Нет свободных курьеров для заказа id=%', p_order_id;
    END IF;

    INSERT INTO delivery_entity (order_id, courier_id, eda_minutes)
    VALUES (p_order_id, v_courier_id, (10 + floor(random() * 35))::INT)
    RETURNING id INTO v_delivery_id;

    RETURN v_delivery_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Ошибка при назначении курьера на заказ id=%: %', p_order_id, SQLERRM;
        RAISE;
END;
$$ LANGUAGE plpgsql;


-- ---------------------------------------------------------------------
-- ФУНКЦИЯ 2. fn_restaurant_revenue_report(restaurant_id, date_from, date_to)
-- Формирует построчный отчёт по доставленным заказам ресторана за
-- период, используя явный КУРСОР (учебная демонстрация темы "курсоры").
-- Для каждого заказа отдельно считается количество позиций через
-- подзапрос (тема "подзапросы").
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_restaurant_revenue_report(
    p_restaurant_id BIGINT,
    p_date_from     DATE,
    p_date_to       DATE
)
RETURNS TABLE (
    order_id     BIGINT,
    delivered_at TIMESTAMP,
    items_count  BIGINT,
    total_amount NUMERIC(19,2)
) AS $$
DECLARE
    cur_orders CURSOR FOR
        SELECT o.id, h.changed_at, o.total_amount
        FROM orders o
        JOIN order_status_history h
             ON h.order_id = o.id AND h.status = 'DELIVERED'
        WHERE o.restaurant_id = p_restaurant_id
          AND h.changed_at::date BETWEEN p_date_from AND p_date_to
        ORDER BY h.changed_at;

    v_order_id     BIGINT;
    v_delivered_at TIMESTAMP;
    v_total_amount NUMERIC(19,2);
BEGIN
    IF NOT EXISTS (SELECT 1 FROM restaurants WHERE id = p_restaurant_id) THEN
        RAISE EXCEPTION 'Ресторан id=% не найден', p_restaurant_id;
    END IF;

    OPEN cur_orders;

    LOOP
        FETCH cur_orders INTO v_order_id, v_delivered_at, v_total_amount;
        EXIT WHEN NOT FOUND;

        order_id     := v_order_id;
        delivered_at := v_delivered_at;
        total_amount := v_total_amount;

        -- количество позиций в заказе — через подзапрос
        SELECT COUNT(*) INTO items_count
        FROM order_item_entity oi
        WHERE oi.order_id = v_order_id;

        RETURN NEXT;
    END LOOP;

    CLOSE cur_orders;
END;
$$ LANGUAGE plpgsql;

-- Пример вызова:
-- SELECT * FROM fn_restaurant_revenue_report(1, '2026-01-01', '2026-12-31');


-- ---------------------------------------------------------------------
-- ПРОЦЕДУРА 3. sp_refund_order(order_id)
-- Отмена/возврат заказа: переводит платёж в REFUNDED и освобождает
-- курьера, если заказ ещё не был доставлен. Демонстрирует
-- транзакционность и обработку исключений в процедуре (не функции).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_refund_order(p_order_id BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_courier_id BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM payments WHERE order_id = p_order_id) THEN
        RAISE EXCEPTION 'Платёж по заказу id=% не найден, возврат невозможен', p_order_id;
    END IF;

    UPDATE payments
    SET payment_status = 'REFUNDED'
    WHERE order_id = p_order_id;

    SELECT courier_id INTO v_courier_id
    FROM delivery_entity
    WHERE order_id = p_order_id;

    IF FOUND THEN
        UPDATE couriers SET available = TRUE WHERE id = v_courier_id;
    END IF;

    RAISE NOTICE 'Заказ id=% возвращён, курьер освобождён', p_order_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'Не удалось выполнить возврат заказа id=%: %', p_order_id, SQLERRM;
        RAISE;
END;
$$;

-- Пример вызова:
-- CALL sp_refund_order(42);
