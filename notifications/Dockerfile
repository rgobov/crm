# ЭТАП 1: Сборка
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Кешируем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

# ЭТАП 2: Запуск (Используем Debian-based образ вместо Alpine для совместимости с TDLib)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Устанавливаем необходимые библиотеки согласно документации TDLib:
# OpenSSL (libssl3), zlib (zlib1g), стандартные библиотеки C++ (libstdc++6) и сертификаты
RUN apt-get update && apt-get install -y \
    libssl3 \
    zlib1g \
    libstdc++6 \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/notifications-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
