FROM python:3.13-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

RUN wget -q https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip \
    && unzip -q vosk-model-small-ru-0.22.zip \
    && rm vosk-model-small-ru-0.22.zip

COPY main.py database.py .

EXPOSE 8082

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8082"]
