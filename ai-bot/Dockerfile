FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN apt-get update && apt-get install -y --no-install-recommends wget ca-certificates tar \
    && echo "Downloading whisper.cpp binary..." \
    && ( for i in 1 2 3; do \
         wget -q https://github.com/ggml-org/whisper.cpp/releases/download/v1.9.1/whisper-bin-ubuntu-x64.tar.gz \
           && break || sleep 5; \
       done; \
       if [ -f whisper-bin-ubuntu-x64.tar.gz ]; then \
         tar xzf whisper-bin-ubuntu-x64.tar.gz \
         && cp whisper-bin-ubuntu-x64/whisper-cli /usr/local/bin/ \
         && cp whisper-bin-ubuntu-x64/*.so* /usr/local/lib/ \
         && ldconfig \
         && rm -rf whisper-bin-ubuntu-x64* \
         && echo "whisper-cli installed"; \
       else \
         echo "WARNING: whisper-cli download failed — voice messages will not work"; \
         touch /opt/whisper-download-failed; \
       fi ) \
    && echo "Downloading whisper base model..." \
    && mkdir -p /opt/whisper \
    && ( for i in 1 2 3; do \
         wget -q -O /opt/whisper/ggml-base.bin \
           https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin \
           && break || sleep 5; \
       done; \
       if [ -s /opt/whisper/ggml-base.bin ]; then \
         echo "whisper model installed"; \
       else \
         echo "WARNING: whisper model download failed — voice messages will not work"; \
         rm -f /opt/whisper/ggml-base.bin; \
       fi ) \
    && apt-get remove -y wget && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
