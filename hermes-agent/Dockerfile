FROM ghcr.io/openclaw/openclaw:latest

WORKDIR /app

# Copy extension plugin
COPY extensions/tryneuro-provider/ ./extensions/tryneuro-provider/

# Install plugin dependencies
RUN cd extensions/tryneuro-provider && npm install --production

# OpenClaw entrypoint is already set in base image