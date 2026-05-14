# ── Etapa 1: Build de React ───────────────────────────────────────────────────
FROM node:18-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --legacy-peer-deps && \
    npm install ajv@^8 --legacy-peer-deps
COPY . .
ARG REACT_APP_BFF_URL=http://localhost:9090/api
ENV REACT_APP_BFF_URL=$REACT_APP_BFF_URL
ENV DISABLE_ESLINT_PLUGIN=true
RUN npm run build

# ── Etapa 2: Servidor Nginx para servir el build ──────────────────────────────
FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
