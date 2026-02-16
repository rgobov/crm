# Этап 1: Сборка
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Установка необходимых нативных библиотек для TDLib
RUN apk add --no-cache libstdc++ gcompat openssl zlib

COPY --from=build /app/target/*.jar app.jar

# Создаем папку для сессий
RUN mkdir tg-sessions && chmod 777 tg-sessions

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
