# CSA 前端生产镜像（Next.js 16 / React 19）
#
# next.config.ts 没有开启 output: 'standalone'，所以这里不走 standalone 产物，
# 而是采用常规的 `next start` 运行方式：构建后携带生产依赖 + .next 产物启动。
#
# 重要：NEXT_PUBLIC_* 变量在 `next build` 时被内联进浏览器端 bundle（构建期固化），
# 运行期再改环境变量对已经打好的前端包无效，因此 NEXT_PUBLIC_API_URL 用 build ARG 传入。

# ---- 1. 依赖安装（含 devDependencies，构建需要） ----
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---- 2. 构建 ----
FROM node:20-alpine AS builder
WORKDIR /app
# 构建期注入 API 地址，会被内联进客户端代码
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---- 3. 仅生产依赖（用于运行期，剔除 dev 依赖，缩小体积） ----
FROM node:20-alpine AS prod-deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# ---- 4. 运行时镜像 ----
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Runner 不需要 npm/npx：构建已经结束，Next.js 可由 node 直接启动。
# 移除基础镜像中未使用的全局 npm 可避免把其传递依赖带入生产攻击面。
RUN apk upgrade --no-cache \
    && rm -rf /usr/local/lib/node_modules/npm \
    && rm -f /usr/local/bin/npm /usr/local/bin/npx \
    && addgroup -g 1001 -S nodejs \
    && adduser -u 1001 -S nextjs -G nodejs

COPY --from=prod-deps /app/node_modules ./node_modules
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/next.config.ts ./next.config.ts
COPY --from=builder /app/package.json ./package.json

USER nextjs

EXPOSE 3000
CMD ["node", "node_modules/next/dist/bin/next", "start"]
