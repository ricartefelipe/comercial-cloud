# node:22-alpine
FROM node@sha256:fe4f6d74e20370acb04ad889077d14d0f64a9c8ea6a42bc60cddf1e844699914 AS base

FROM base AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci

FROM base AS builder
WORKDIR /app
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ARG NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8180
ARG NEXT_PUBLIC_KEYCLOAK_REALM=comercialcloud
ARG NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=comercial-cloud-web
# keycloak (produção), demo (demonstração pública com a conta de demonstração) ou header (desenvolvimento local).
ARG NEXT_PUBLIC_AUTH_MODE=keycloak
ARG NEXT_PUBLIC_DEMO_EMAIL=
ARG NEXT_PUBLIC_DEMO_SENHA=
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_KEYCLOAK_URL=$NEXT_PUBLIC_KEYCLOAK_URL
ENV NEXT_PUBLIC_KEYCLOAK_REALM=$NEXT_PUBLIC_KEYCLOAK_REALM
ENV NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=$NEXT_PUBLIC_KEYCLOAK_CLIENT_ID
ENV NEXT_PUBLIC_AUTH_MODE=$NEXT_PUBLIC_AUTH_MODE
ENV NEXT_PUBLIC_DEMO_EMAIL=$NEXT_PUBLIC_DEMO_EMAIL
ENV NEXT_PUBLIC_DEMO_SENHA=$NEXT_PUBLIC_DEMO_SENHA
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# O servidor standalone só precisa do node: npm, npx e corepack ficam fora da imagem final, com as falhas que trazem.
RUN apk upgrade --no-cache \
  && rm -rf /usr/local/lib/node_modules /usr/local/bin/npm /usr/local/bin/npx /usr/local/bin/corepack /opt/yarn* \
  && addgroup --system --gid 1001 nodejs \
  && adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
