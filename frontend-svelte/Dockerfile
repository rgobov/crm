FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/package*.json ./
COPY --from=build /app/build ./build
COPY --from=build /app/node_modules ./node_modules

# Работаем на порту 5000
ENV PORT=5000
ENV HOST=0.0.0.0
ENV NODE_ENV=production
EXPOSE 5000

# Запускаем напрямую, минуя npm для стабильности сигналов
CMD ["node", "build/index.js"]
