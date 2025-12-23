# Используем легкий образ с Java 17
FROM eclipse-temurin:17-jdk-alpine

# Рабочая директория
WORKDIR /app

# Копируем собранный JAR файл в контейнер
# Убедитесь, что mvn clean package был выполнен перед сборкой образа
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar

# Указываем команду для запуска
ENTRYPOINT ["java", "-jar", "app.jar"]
