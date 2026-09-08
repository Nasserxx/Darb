# --- Build stage ---
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Deployment overrides:
#   VITE_API_URL  - base URL of the backend API ("" = same origin, e.g. behind a proxy)
#   VITE_BASE_PATH - base path the SPA is served under ("/" for root deployments)
ARG VITE_API_URL=""
ARG VITE_BASE_PATH="/"
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_BASE_PATH=$VITE_BASE_PATH

RUN npm run build

# --- Serve stage ---
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
