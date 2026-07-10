FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN apt-get update && apt-get install -y --no-install-recommends wget ca-certificates \
    && wget -q -O /usr/local/bin/whisper-cli \
       https://github.com/ggml-org/whisper.cpp/releases/download/v1.9.1/whisper-cli \
    && chmod +x /usr/local/bin/whisper-cli \
    && mkdir -p /opt/whisper \
    && wget -q -O /opt/whisper/ggml-base.bin \
       https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin \
    && apt-get remove -y wget && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
