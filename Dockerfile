FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Исправление проблемы с сертификатами Maven Central
RUN apt-get update && apt-get install -y ca-certificates && \
    update-ca-certificates && \
    keytool -importkeystore -srckeystore /etc/ssl/certs/java/cacerts -destkeystore /usr/lib/jvm/java-21-openjdk-amd64/lib/security/cacerts \
    -srcstorepass changeit -deststorepass changeit -noprompt || true

# Копируем pom.xml сначала (для кэширования зависимостей)
COPY pom.xml .

# Скачиваем зависимости с отключением строгой проверки SSL (временный флаг)
RUN mvn dependency:go-offline -B \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true

# Копируем исходный код
COPY . .

# Собираем приложение в production-режиме для Vaadin 25
RUN mvn clean package -Pproduction \
    -Dmaven.test.skip=true \
    -Dvaadin.productionMode=true \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true

# ==================== Stage 2: Runtime ====================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем только готовый JAR
COPY --from=builder /app/target/fimas-0.0.1.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
