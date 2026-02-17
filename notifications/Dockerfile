# ЭТАП 1: Сборка
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Кешируем зависимости
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests

# ЭТАП 2: Запуск (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Устанавливаем необходимые библиотеки для работы TDLib в Linux
RUN apk add --no-cache gcompat libstdc++

COPY --from=build /app/target/notifications-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
