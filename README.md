# Yandex Market Demo

## Запуск

Нужны JDK 21 и Docker Compose. Из корня проекта поднимите PostgreSQL и Kafka
из основного `docker-compose.yaml`:

```bash
docker compose up -d --wait
```

Для чистой базы создайте схему, триггеры и тестовые данные для Postman:

```bash
docker compose exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U Viar -d YandexMarketDB < db-scripts/01_ddl_schema.sql
docker compose exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U Viar -d YandexMarketDB < db-scripts/04_triggers.sql
docker compose exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U Viar -d YandexMarketDB < db-scripts/06_demo_data.sql
```

Демо-скрипт также добавляет тестовых курьеров и делает доступными тех из них,
у кого сейчас нет заказа со статусом `DELIVERY_ASSIGNED`.

Повторно запускать `up` с другим именем проекта и `docker-compose.example.yaml`
не нужно: это второй стек, который попытается занять тот же порт PostgreSQL `5433`.
Чтобы проверить состояние контейнеров основного стека, используйте `docker compose ps`.

Запустите каждый сервис в отдельном терминале:

```bash
./gradlew :payment-service:bootRun
./gradlew :delivery-service:bootRun
./gradlew :order-service:bootRun
```

Порты: PostgreSQL `5433`, payment `8081`, order `8082`, delivery `8083`.
Если у PostgreSQL уже есть volume, `POSTGRES_USER` и `POSTGRES_PASSWORD` из Compose
не меняют созданные ранее учетные данные. В таком случае задайте `DB_USER` и
`DB_PASSWORD` для запуска сервисов с реальными учетными данными существующей БД.

## Проверка в Postman

Создайте запрос `POST http://localhost:8082/api/orders` с телом:

```json
{
  "customerId": 900001,
  "addressId": 900001,
  "restaurantId": 900001,
  "items": [
    {
      "productId": 900001,
      "quantity": 1
    }
  ]
}
```

В Tests запроса сохраните идентификатор для следующих запросов:

```javascript
const order = pm.response.json();
pm.environment.set("orderId", order.id);
pm.test("Order created with PENDING_PAYMENT", function () {
    pm.response.to.have.status(200);
    pm.expect(order.orderStatus).to.eql("PENDING_PAYMENT");
});
```

Далее выполните по порядку:

1. `GET http://localhost:8082/api/orders/{{orderId}}` — ожидайте `PENDING_PAYMENT`.
2. `POST http://localhost:8082/api/orders/{{orderId}}/pay` с телом `{"paymentMethod":"CARD"}` — ожидайте `PAID`.
3. Повторяйте GET через пару секунд, пока Kafka не установит `DELIVERY_ASSIGNED`.
4. `POST http://localhost:8082/api/orders/{{orderId}}/deliver` без тела — ожидайте `DELIVERED`.

Для проверки QR создайте отдельный заказ, сохраните его ID в `qrOrderId` и
выполните `POST /api/orders/{{qrOrderId}}/pay` с телом
`{"paymentMethod":"QR"}`. Ожидаемый статус — `PAYMENT_FAILED`; доставка не назначается.

Тесты проекта запускаются командой:

```bash
./gradlew test :order-service:test :delivery-service:test :payment-service:test
```
