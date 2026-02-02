# ЭТАП 1: Сборка
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Кешируем зависимости (не скачиваем их каждый раз)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Копируем исходный код
COPY src ./src

# ОПТИМИЗИРОВАННАЯ СБОРКА:
# 1. Убираем 'clean', чтобы не удалять кэш скомпилированных классов
# 2. Используем кэш BuildKit для папки .m2 (библиотеки)
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests

# ЭТАП 2: Запуск (Минимальный JRE)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Берем JAR файл из этапа сборки
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
