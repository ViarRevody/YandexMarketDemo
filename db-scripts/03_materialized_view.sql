-- =====================================================================
-- 03_materialized_view.sql
-- Материализованное представление — отдельная тема курса.
-- Используется там, где агрегирующий запрос дорогой и данные для
-- отчёта не обязаны быть на 100% актуальными в реальном времени
-- (например, дашборд для менеджера ресторана, обновляемый по расписанию).
-- =====================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_restaurant_daily_revenue AS
SELECT
    r.id                         AS restaurant_id,
    r.name                       AS restaurant_name,
    date_trunc('day', h.changed_at)::date AS revenue_date,
    COUNT(DISTINCT o.id)         AS orders_count,
    SUM(o.total_amount)          AS total_revenue
FROM orders o
JOIN restaurants r ON r.id = o.restaurant_id
JOIN order_status_history h
     ON h.order_id = o.id AND h.status = 'DELIVERED'
GROUP BY r.id, r.name, date_trunc('day', h.changed_at)
WITH DATA;

-- Уникальный индекс необходим, чтобы можно было обновлять
-- представление конкурентно (REFRESH ... CONCURRENTLY), не блокируя
-- на это время читателей отчёта.
CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_restaurant_daily_revenue
    ON mv_restaurant_daily_revenue (restaurant_id, revenue_date);

-- Пример обновления данных (запускать по расписанию, например через
-- cron / pg_cron, раз в час или раз в сутки):
--
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_restaurant_daily_revenue;
