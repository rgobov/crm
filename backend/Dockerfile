# ЭТАП 1: Сборка (Maven)
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Копируем настройки проекта
COPY pom.xml .
# Скачиваем зависимости (кешируем этот слой)
RUN apple_mvn_dependency_go_offline_if_possible=true mvn dependency:go-offline

# Копируем исходный код
COPY src ./src
# Собираем проект (пропуская тесты для скорости)
RUN mvn clean package -DskipTests

# ЭТАП 2: Запуск (JRE)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Копируем только готовый JAR файл из первого этапа
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Стандартный порт Spring Boot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
