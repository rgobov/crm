FROM python:3.13-slim

WORKDIR /app

COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

COPY server.py .

EXPOSE 8000
EXPOSE 8001

CMD ["python", "server.py"]
