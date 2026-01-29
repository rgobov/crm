# ЭТАП 1: Сборка (Node.js)
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# ЭТАП 2: Запуск
FROM node:20-alpine
WORKDIR /app
# Копируем результаты сборки и нужные модули
COPY --from=build /app/package*.json ./
COPY --from=build /app/node_modules ./
COPY --from=build /app/build ./build

ENV PORT=3000
EXPOSE 3000

CMD ["node", "build/index.js"]
