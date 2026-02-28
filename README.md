# My Market App

## 📋 Описание проекта

My Market App - это микросервисное приложение для управления маркетплейсом с функционалом:
- **market-app** - основное приложение с каталогом товаров, корзиной и заказами
- **payment-service** - микросервис для обработки платежей
- **Docker Compose** - конфигурация для развертывания полного стека

### Архитектура
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  market-app │    │payment-service│    │    Redis    │
│   (8080)   │    │   (8081)    │    │  (6379)    │
│             │    │             │    │             │
│  Spring     │    │   Spring     │    │   Cache     │
│ WebFlux     │    │  WebFlux     │    │             │
│             │    │             │    │             │
│     H2      │    │     H2      │    │             │
│   (9092)    │    │   (9093)    │    │             │
└─────────────┘    └─────────────┘    └─────────────┘
```

## 🚀 Docker Compose

Docker Compose конфигурация для запуска полного стека приложений:
- **market-app** - основное приложение (порт 8080)
- **payment-service** - сервис платежей (порт 8081)
- **market-db** - база данных для market-app (H2, порт 9092)
- **payment-db** - база данных для payment-service (H2, порт 9093)
- **market-redis** - Redis для кеширования (порт 6379)

## 🚀 Быстрый запуск

```bash
# Запуск всех сервисов
docker-compose up -d

# Просмотр логов
docker-compose logs -f

# Остановка всех сервисов
docker-compose down
```

## 📂 Структура

```
my-market-app/
├── docker-compose.yml              # Основной файл конфигурации
├── market-app/
│   ├── Dockerfile                 # Сборка market-app
│   ├── .dockerignore             # Исключения для Docker
│   └── src/main/resources/
│       └── application-docker.properties  # Конфигурация для Docker
└── payment-service/
    ├── Dockerfile                 # Сборка payment-service
    ├── .dockerignore             # Исключения для Docker
    └── src/main/resources/
        └── application-docker.properties  # Конфигурация для Docker
```

## 🔧 Конфигурация

### Переменные окружения

**market-app:**
- `SPRING_DATASOURCE_URL` - URL подключения к H2
- `SPRING_REDIS_HOST` - хост Redis
- `PAYMENT_SERVICE_URL` - URL сервиса платежей

**payment-service:**
- `SPRING_DATASOURCE_URL` - URL подключения к H2
- `payment.initial.balance` - начальный баланс (1,000,000.00)

### Порты

| Сервис | Внутренний порт | Внешний порт |
|--------|----------------|--------------|
| market-app | 8080 | 8080 |
| payment-service | 8081 | 8081 |
| market-db | 9092 | 9092 |
| payment-db | 9092 | 9093 |
| market-redis | 6379 | 6379 |

## 🗂️ Тома данных

- `market-data` - данные H2 для market-app
- `payment-data` - данные H2 для payment-service  
- `market-redis-data` - данные Redis

## 🌐 Доступ к сервисам

После запуска:

- **Основное приложение**: http://localhost:8080
- **Сервис платежей**: http://localhost:8081
- **H2 Console (market)**: http://localhost:9092
- **H2 Console (payment)**: http://localhost:9093
- **Redis**: localhost:6379

## 📝 Полезные команды

```bash
# Пересборка и запуск
docker-compose up --build -d

# Запуск только определенных сервисов
docker-compose up -d market-app market-redis

# Просмотр логов конкретного сервиса
docker-compose logs -f market-app

# Вход в контейнер
docker-compose exec market-app bash

# Очистка томов и контейнеров
docker-compose down -v
```

## 🐛 Отладка

### Просмотр логов
```bash
# Все логи
docker-compose logs

# Логи конкретного сервиса
docker-compose logs market-app
docker-compose logs payment-service
```

### Проверка здоровья сервисов
```bash
# Проверка доступности
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Проверка сервиса платежей
curl http://localhost:8081/api/balance
```

### Базы данных
- **H2 Console**: http://localhost:9092 (JDBC URL: `jdbc:h2:tcp://localhost/market`)
- **H2 Console Payment**: http://localhost:9093 (JDBC URL: `jdbc:h2:tcp://localhost/payment`)

## ⚠️ Важные замечания

1. **Первый запуск** может занять время из-за сборки образов
2. **Порядок запуска**: базы данных запускаются первыми
3. **Перезапуск кода**: используйте `docker-compose up --build` для пересборки
4. **Логи**: все логи пишутся в директорию `./logs` на хосте
5. **Профиль**: используется `docker` профиль для конфигурации

## 🔒 Безопасность

В продакшн среде рекомендуется:
- Изменить пароли по умолчанию
- Ограничить доступ к H2 console
- Использовать HTTPS
- Настроить сети Docker

---

## 💻 Разработка проекта

### 📂 Структура проекта

```
my-market-app/
├── pom.xml                           # Корневой Maven файл
├── market-app/                        # Основное приложение
│   ├── pom.xml                       # Maven конфигурация
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/iron/mymarket/
│   │   │   │   ├── controller/          # Контроллеры
│   │   │   │   ├── CartController.java
│   │   │   │   ├── ItemsController.java
│   │   │   │   └── OrdersController.java
│   │   │   ├── service/              # Сервисы
│   │   │   │   ├── CartService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── PaymentClientService.java
│   │   │   │   └── PaymentHealthService.java
│   │   │   ├── dao/                  # Доступ к данным
│   │   │   │   └── repository/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── templates/          # Thymeleaf шаблоны
│   │   └── test/                    # Тесты
│   └── Dockerfile
└── payment-service/                   # Сервис платежей
    ├── pom.xml
    ├── src/
    │   ├── main/
    │   │   └── java/com/iron/payment/
    │   │       ├── PaymentController.java
    │   │       ├── PaymentService.java
    │   │       └── model/
    │   └── test/
    └── Dockerfile
```

### 🔨 Сборка мультипроекта

```bash
# Сборка всего проекта
./mvnw clean install

# Сборка только market-app
./mvnw clean install -pl market-app

# Сборка только payment-service  
./mvnw clean install -pl payment-service

# Сборка с пропуском тестов
./mvnw clean install -DskipTests

# Генерация OpenAPI клиента для market-app
./mvnw generate-sources -pl market-app
```

### 🧪 Запуск тестов

```bash
# Запуск всех тестов
./mvnw test

# Тесты только market-app
./mvnw test -pl market-app

# Тесты только payment-service
./mvnw test -pl payment-service

# Запуск конкретного теста
./mvnw test -pl market-app -Dtest=CartControllerTest

# Запуск с покрытием
./mvnw test jacoco:report -pl market-app
```

### 🚀 Локальный запуск

#### Запуск основных сервисов

```bash
# Запуск payment-service (порт 8081)
cd payment-service
../mvnw spring-boot:run

# Запуск market-app (порт 8080) в другом терминале
cd market-app  
../mvnw spring-boot:run
```

#### Запуск с Redis

```bash
# Запуск Redis через Docker
docker run -d --name redis -p 6379:6379 redis:7-alpine

# Или через системный Redis (если установлен)
redis-server
```

#### Конфигурация для локальной разработки

**market-app/application.properties:**
```properties
# База данных H2
spring.datasource.url=jdbc:h2:mem:market;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=password

# Redis (если запущен локально)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Сервис платежей
payment.service.url=http://localhost:8081
```

**payment-service/application.properties:**
```properties
# База данных H2
spring.datasource.url=jdbc:h2:mem:payment;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=password

# Начальный баланс
payment.initial.balance=1000.00
```

### 🌐 Использование проекта

#### Основные эндпоинты

**market-app (http://localhost:8080):**
- `GET /` - главная страница с товарами
- `GET /items` - каталог товаров (JSON API)
- `GET /cart/items` - корзина пользователя
- `POST /cart/items` - изменение количества товаров
- `POST /buy` - оформление заказа
- `GET /orders` - список заказов
- `GET /orders/{id}` - детализация заказа

**payment-service (http://localhost:8081):**
- `GET /api/balance` - текущий баланс
- `POST /api/pay` - выполнение платежа
- `GET /actuator/health` - проверка здоровья

#### Примеры использования

```bash
# Получить каталог товаров
curl http://localhost:8080/items

# Добавить товар в корзину (через web интерфейс)
# Открыть http://localhost:8080 и добавить товары

# Оформить заказ
curl -X POST http://localhost:8080/buy \
  -H "Content-Type: application/x-www-form-urlencoded"

# Проверить баланс
curl http://localhost:8081/api/balance

# Выполнить платеж
curl -X POST http://localhost:8081/api/pay \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00}'
```

### 🔧 Полезные команды разработки

```bash
# Очистка и пересборка
./mvnw clean compile

# Форматирование кода (если настроено)
./mvnw spotless:apply

# Проверка зависимостей
./mvnw dependency:tree

# Анализ кода
./mvnw sonar:sonar

# Создание дистрибутива
./mvnw clean package
```

### 🐛 Отладка

#### IDE Configuration
- **IntelliJ IDEA**: Import as Maven Project
- **VS Code**: Maven for Java extension
- **Eclipse**: Import as Existing Maven Project

#### Health Checks
```bash
# Проверка доступности сервисов
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Проверка Redis
redis-cli ping
```

#### Логи
```bash
# Просмотр логов Spring Boot
tail -f market-app/logs/spring.log

# Логи в реальном времени
./mvnw spring-boot:run | grep DEBUG
```

### 📊 Мониторинг

#### Actuator Endpoints
- `/actuator/health` - здоровье приложения
- `/actuator/info` - информация о приложении  
- `/actuator/metrics` - метрики производительности
- `/actuator/env` - переменные окружения

#### Базы данных
- **H2 Console**: http://localhost:8080/h2-console (market-app)
- **H2 Console**: http://localhost:8081/h2-console (payment-service)

### 🚀 Продакшн развертывание

```bash
# Сборка Docker образов
docker build -t market-app:latest ./market-app
docker build -t payment-service:latest ./payment-service

# Запуск в продакшн режиме
docker-compose -f docker-compose.prod.yml up -d

```

### 📚 Дополнительные ресурсы

- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/
- **Spring WebFlux Guide**: https://spring.io/guides/gs/reactive-rest-service/
- **Docker Compose Reference**: https://docs.docker.com/compose/
- **H2 Database**: http://www.h2database.com/html/main.html
- **Redis Documentation**: https://redis.io/documentation
