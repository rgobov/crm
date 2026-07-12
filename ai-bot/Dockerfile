FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

FROM maven:3.9-eclipse-temurin-21 AS whisper-build
ARG BUILD_PROXY
ENV https_proxy=${BUILD_PROXY}
RUN apt-get update && apt-get install -y --no-install-recommends git ca-certificates cmake build-essential && \
    git clone --depth 1 --branch v1.9.1 https://github.com/ggml-org/whisper.cpp.git /whisper && \
    mkdir /whisper/build && cd /whisper/build && \
    cmake .. -DBUILD_SHARED_LIBS=OFF -DGGML_CUDA=OFF -DGGML_VULKAN=OFF -DGGML_METAL=OFF && \
    cmake --build . --target whisper-cli -j$(nproc --all)

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=whisper-build /whisper/build/bin/whisper-cli /usr/local/bin/
ARG BUILD_PROXY
ENV https_proxy=${BUILD_PROXY}
RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 wget ca-certificates ffmpeg && \
    mkdir -p /opt/whisper && \
    echo "Downloading whisper base model..." && \
    ( for i in 1 2 3; do \
         wget -q -O /opt/whisper/ggml-base.bin \
           https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin \
           && break || sleep 5; \
       done; \
       if [ -s /opt/whisper/ggml-base.bin ]; then \
         echo "whisper model installed"; \
       else \
         echo "WARNING: whisper model download failed — voice messages will not work"; \
         rm -f /opt/whisper/ggml-base.bin; \
       fi ) && \
    apt-get remove -y wget && apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
