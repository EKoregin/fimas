# FIMAS — Firewall Policy Management System

Приложение для управления политиками межсетевых экранов.

# Развертывание приложения с использованием Docker Compose (самый простой способ)
1. Установите **Docker Desktop** (Windows / macOS) или Docker + Docker Compose (Linux)
2. Клонируйте репозиторий

```bash
git clone https://github.com/твой-username/fimas.git
cd fimas
```
2. Выполните в папке проекта всего **одну команду**:

```bash
docker compose up -d
```

3. Откройте в браузере: http://localhost:8085

Приложение и база данных запустятся автоматически.


# Развертывание приложения из исходного кода
## Требования

- **Java 21** или выше
- **Maven 3.8+**
- **PostgreSQL 14** или выше
- Git

---

## 1. Клонирование репозитория

```bash
git clone https://github.com/твой-username/fimas.git
cd fimas
```


## 2. Установка и запуск PostgreSQL

### Вариант 1: Docker (рекомендуется для всех ОС)
 ```bash
docker run --name fimas-postgres \
  -e POSTGRES_DB=fimas \
  -e POSTGRES_USER=fimas \
  -e POSTGRES_PASSWORD=fimas \
  -p 5432:5432 \
  -d postgres:18
```

### Вариант 2: Установка PostgreSQL вручную
#### Windows
1. Скачайте и установите PostgreSQL с официального сайта: https://www.postgresql.org/download/windows/
2. Во время установки запомните пароль для пользователя postgres
3. Откройте pgAdmin или утилиту psql и выполните:

```sql
CREATE DATABASE fimas;
CREATE USER fimas WITH PASSWORD 'fimas';
ALTER USER fimas WITH SUPERUSER;
GRANT ALL PRIVILEGES ON DATABASE fimas TO fimas;
```
#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib -y

sudo -u postgres psql <<EOF
CREATE DATABASE fimas;
CREATE USER fimas WITH PASSWORD 'fimas123';
ALTER USER fimas WITH SUPERUSER;
GRANT ALL PRIVILEGES ON DATABASE fimas TO fimas;
\q
EOF
```
## 3. Настройка приложения
По умолчанию приложение использует:
* порт 8085
* пользователь/пароль - fimas/fimas

Если хотите поменять, от добавьте файл application-local.yml в корне проекта (рядом с pom.xml):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fimas
    username: fimas
    password: fimas123

server:
  port: 8080
```

## 4. Запуск приложения
### Сборка и запуск JAR-файла

```bash
mvn clean package -DskipTests
```
#### Windows
```bash
java -jar target\fimas-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

#### Linux
```bash
java -jar target/fimas-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Проверка работы
После запуска откройте в браузере:
* Приложение: http://localhost:8085 (или свой порт, если меняли)

## Полезные команды
```bash
# Пересборка проекта
mvn clean package

# Запуск с указанием профиля
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Возможные проблемы и решения

| Проблема                          | Решение |
|-----------------------------------|--------|
| Порт 5432 занят                   | Убедитесь, что PostgreSQL не запущен в другом контейнере или сервисе. Можно изменить порт в команде Docker или в `application-local.yml`. |
| Ошибка подключения к базе данных  | Проверьте правильность `url`, `username` и `password` в файле `application-local.yml`. Убедитесь, что PostgreSQL запущен. |
| Порт 8080 занят                   | Добавьте строку `server.port=8085` (или другой свободный порт) в файл `application-local.yml`. |
| Flyway не применяет миграции      | Убедитесь, что в `application-local.yml` присутствует:<br>`spring.flyway.enabled: true` |
| Команда `mvn spring-boot:run` не работает | Убедитесь, что Maven установлен и добавлен в PATH. Попробуйте выполнить `mvn clean install` сначала. |
| Java не найдена                   | Убедитесь, что Java 17 установлена и переменная `JAVA_HOME` настроена правильно. Проверьте версию командой `java -version`. |

