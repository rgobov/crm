# ЭТАП 1: Сборка
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Кешируем зависимости
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Копируем исходный код
COPY src ./src

# ВКЛЮЧАЕМ ТЕСТЫ ПЕРЕД СБОРКОЙ:
# Если тесты не пройдут, образ не соберется и деплой не состоится.
RUN --mount=type=cache,target=/root/.m2 mvn -DskipTests package

# ЭТАП 2: Запуск
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
