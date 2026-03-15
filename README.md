# My Market App

## 📋 Описание проекта

My Market App - это микросервисное приложение для управления маркетплейсом с системой аутентификации через Keycloak:

### 🏗️ Архитектура

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  market-app │    │payment-service│  │   Keycloak  │    │    Redis    │
│   (8080)    │    │   (8081)    │    │   (8082)    │    │  (6379)     │
│             │    │             │    │             │    │             │
│  Spring     │    │   Spring    │    │   Identity  │    │   Cache     │
│ WebFlux     │    │  WebFlux    │    │   Provider  │    │             │
│             │    │             │    │             │    │             │
│     H2      │    │     H2      │    │             │    │             │
│   (9092)    │    │   (9093)    │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### 🔐 Система авторизации

- **Keycloak** - сервер идентификации (порт 8082)
- **OAuth2** - протокол авторизации между сервисами
- **OpenID Connect** - аутентификация пользователей
- **JWT токены** - безопасная передача данных между сервисами

### 📦 Микросервисы

- **market-app** - основное приложение с каталогом товаров, корзиной и заказами
- **payment-service** - микросервис для обработки платежей с OAuth2 защитой
- **payment-api** - OpenAPI спецификация для сервиса платежей

## 🚀 Запуск приложения

### ⚠️ Важное замечание
Сборка и запуск приложения возможны **только через Docker Compose**. Локальный запуск не поддерживается из-за сложной конфигурации Keycloak и зависимостей между сервисами.

### 🐳 Docker Compose (рекомендуемый способ)

```bash
# Запуск полного стека приложений
docker-compose up -d

# Просмотр логов
docker-compose logs -f

# Остановка всех сервисов
docker-compose down
```

#### 🚀 Что запускается:

- **market-app** - основное приложение (порт 8080)
- **payment-service** - сервис платежей (порт 8081) 
- **keycloak** - сервер идентификации (порт 8082)
- **market-db** - база данных H2 для market-app (порт 9092)
- **payment-db** - база данных H2 для payment-service (порт 9093)
- **keycloak-db** - PostgreSQL для Keycloak (порт 5432)
- **market-redis** - Redis для кеширования (порт 6379)

## 🔐 Авторизация и безопасность

### 📋 Конфигурация Keycloak

- **Realm**: `my-market`
- **Client**: `market-app` (public client)
- **Client**: `payment-service` (confidential client)
- **Test users**: преднастроенные пользователи для демо

### 🔑 Процесс авторизации

1. **Пользователь** заходит на http://localhost:8080
2. **Перенаправление** на страницу входа Keycloak
3. **Аутентификация** через логин/пароль
4. **Получение JWT токена** для доступа к ресурсам
5. **OAuth2** для взаимодействия между сервисами

### 🛡️ Защита эндпоинтов

- **market-app**: защищенные эндпоинты `/cart/**`, `/orders/**`
- **payment-service**: все эндпоинты защищены OAuth2
- **Межсервисное взаимодействие**: только с валидными JWT токенами

## 🌐 Доступ к сервисам

После запуска:

- **Основное приложение**: http://localhost:8080
  - Пользователь: `test_user`
  - Пароль: `password`
- **Сервис платежей**: http://localhost:8081 (только через OAuth2)
- **Keycloak Admin Console**: http://localhost:8082/admin
  - Пользователь: `admin`
  - Пароль: `admin`
- **Keycloak Account Console**: http://localhost:8082/realms/my-market/account
- **H2 Console (market)**: http://localhost:9092
- **H2 Console (payment)**: http://localhost:9093

## 📂 Структура проекта

```
my-market-app/
├── docker-compose.yml              # Основная конфигурация всех сервисов
├── Dockerfile                      # Multi-stage build для production
├── realm-export.json               # Конфигурация Keycloak realm
├── market-app/                     # Основное приложение
│   ├── src/main/java/
│   │   └── com/iron/mymarket/
│   │       ├── controller/          # REST контроллеры
│   │       ├── service/           # Бизнес-логика
│   │       ├── configuration/     # Конфигурация безопасности
│   │       └── security/          # OAuth2 настройки
│   └── src/test/                   # Тесты безопасности
├── payment-service/                # Сервис платежей
│   ├── src/main/java/
│   │   └── com/iron/
│   │       ├── controller/          # REST API
│   │       ├── service/           # Логика платежей
│   │       └── configuration/     # OAuth2 resource server
│   └── src/test/                   # Тесты OAuth2 защиты
└── payment-api/                     # OpenAPI спецификация
    └── src/main/resources/
        └── openapi/
            └── payment-api.yaml   # API спецификация
```

## 🔧 Конфигурация

### 🌍 Переменные окружения

**market-app:**
- `SPRING_DATASOURCE_URL` - URL подключения к H2
- `SPRING_REDIS_HOST` - хост Redis
- `PAYMENT_SERVICE_URL` - URL сервиса платежей
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUER_URI` - URL Keycloak

**payment-service:**
- `SPRING_DATASOURCE_URL` - URL подключения к H2
- `payment.initial.balance` - начальный баланс
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` - URL Keycloak

### 📋 Порты

| Сервис | Внутренний порт | Внешний порт |
|--------|----------------|--------------|
| market-app | 8080 | 8080 |
| payment-service | 8081 | 8081 |
| keycloak | 8080 | 8082 |
| market-db | 9092 | 9092 |
| payment-db | 9093 | 9093 |
| market-redis | 6379 | 6379 |

## 🗂️ Тома данных

- `market-data` - данные H2 для market-app
- `payment-data` - данные H2 для payment-service
- `market-redis-data` - данные Redis

## 📝 Полезные команды

```bash
# Полная пересборка и запуск
docker-compose up --build -d

# Запуск только определенных сервисов
docker-compose up -d market-app keycloak

# Просмотр логов конкретного сервиса
docker-compose logs -f market-app
docker-compose logs -f keycloak

# Вход в контейнер
docker-compose exec market-app bash
docker-compose exec keycloak bash

# Очистка томов и контейнеров
docker-compose down -v

# Перезагрузка Keycloak с новой конфигурацией
docker-compose restart keycloak
```

## 🧪 Тестирование безопасности

### 🔐 Тесты OAuth2

Проект включает комплексные тесты безопасности:

- **market-app**: тесты конфигурации безопасности и OAuth2 аутентификации
- **payment-service**: тесты защиты эндпоинтов через OAuth2
- **Интеграционные тесты**: проверка взаимодействия сервисов

```bash
# Запуск тестов безопасности
./mvnw test -pl market-app -Dtest=SecurityConfigTest
./mvnw test -pl market-app -Dtest=OAuth2AuthenticationTest
./mvnw test -pl payment-service -Dtest=PaymentControllerTest
```

### 🛡️ Проверка защиты

```bash
# Попытка доступа без токена (должен вернуть 401)
curl http://localhost:8081/payments/balance

# Доступ к защищенным эндпоинтам market-app (требует авторизации)
curl http://localhost:8080/cart/items
```

## 🐛 Отладка и мониторинг

### 📊 Health Checks

```bash
# Проверка здоровья сервисов
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Проверка доступности Keycloak
curl http://localhost:8082/realms/my-market/.well-known/openid_configuration
```

### 🔍 Логи

```bash
# Логи конкретного сервиса
docker-compose logs -f market-app
docker-compose logs -f payment-service
docker-compose logs -f keycloak

# Все логи
docker-compose logs
```

### 🗄️ Базы данных

- **H2 Console (market)**: http://localhost:9092 (JDBC URL: `jdbc:h2:tcp://localhost/market`)
- **H2 Console (payment)**: http://localhost:9093 (JDBC URL: `jdbc:h2:tcp://localhost/payment`)
- **Keycloak Admin**: http://localhost:8082/admin (admin/admin)

## ⚠️ Важные замечания

1. **Обязательная зависимость**: Keycloak должен быть запущен перед другими сервисами
2. **Первый запуск**: может занять 5-10 минут из-за инициализации Keycloak
3. **OAuth2 токены**: имеют ограниченное время жизни (1 час по умолчанию)
4. **Безопасность**: все межсервисные взаимодействия защищены OAuth2
5. **Данные**: сохраняются в Docker томах при перезапуске

## 🏗️ Разработка

### 📋 Требования

- Docker и Docker Compose
- Не менее 4GB RAM для всех сервисов
- Свободные порты: 8080, 8081, 8082, 9092, 9093, 5432, 6379

### 🔧 Изменение конфигурации Keycloak

1. Измените `realm-export.json`
2. Выполните: `docker-compose restart keycloak`
3. Импортируйте новую конфигурацию в Keycloak Admin Console

### 📚 Дополнительные ресурсы

- **Keycloak Documentation**: https://www.keycloak.org/documentation
- **Spring Security OAuth2**: https://spring.io/guides/gs/securing-web/
- **Spring WebFlux**: https://spring.io/guides/gs/reactive-rest-service/
- **Docker Compose**: https://docs.docker.com/compose/
- **OpenAPI Specification**: https://swagger.io/specification/
