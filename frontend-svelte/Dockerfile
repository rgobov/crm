# Этап сборки
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Этап запуска
FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/package*.json ./
COPY --from=build /app/node_modules ./
COPY --from=build /app/build ./build

# ПРИНУДИТЕЛЬНО СТАВИМ ПОРТ 80
ENV PORT=80
EXPOSE 80

CMD ["node", "build"]
