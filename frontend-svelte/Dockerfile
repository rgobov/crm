# ЭТАП 1: Сборка
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# Используем --include=dev чтобы точно захватить всё для сборки
RUN --mount=type=cache,target=/root/.npm npm install
COPY . .
RUN npm run build

# ЭТАП 2: Запуск
FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/package*.json ./
# Копируем вообще все модули для надежности
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/build ./build

ENV PORT=3000
EXPOSE 3000

CMD ["node", "build/index.js"]
