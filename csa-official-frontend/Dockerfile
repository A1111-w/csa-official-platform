# Vue 3 compiles to static assets. API requests stay on the same origin.
FROM node:22-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL=/
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

FROM nginxinc/nginx-unprivileged:stable-alpine AS runner
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 3000
