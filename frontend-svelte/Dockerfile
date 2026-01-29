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

# Принудительно устанавливаем порт 80 для Node.js
ENV PORT=80
ENV HOST=0.0.0.0
EXPOSE 80

# Запускаем напрямую через node, чтобы избежать лишних слоев npm
CMD ["node", "build"]
