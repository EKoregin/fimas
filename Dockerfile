#FROM maven:4.0.0-rc-5-eclipse-temurin-21
#RUN mkdir fimas
#WORKDIR fimas
#COPY . .
#RUN mvn package -Dmaven.test.skip=true
#EXPOSE 8085
#CMD ["java", "-jar", "target/fimas-0.0.1.jar"]

# ==================== Stage 1: Build ====================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Копируем только pom.xml сначала (для кэширования зависимостей)
COPY pom.xml .
# Скачиваем зависимости (кэшируется, если pom не менялся)
RUN mvn dependency:go-offline -B

# Теперь копируем весь исходный код
COPY . .

# Важно для Vaadin 25: production build фронтенда
RUN mvn clean package -Pproduction -Dmaven.test.skip=true -Dvaadin.productionMode=true

# ==================== Stage 2: Runtime (лёгкий образ) ====================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем только готовый JAR из builder-stage
COPY --from=builder /app/target/fimas-0.0.1.jar app.jar

EXPOSE 8085

# Оптимизированный запуск Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]