# ===== Stage 1: Build Angular =====
FROM node:20-alpine AS builder

WORKDIR /app

# Copier les fichiers package
COPY package*.json ./

# Installer les dépendances
RUN npm ci --prefer-offline --no-audit

# Copier le code source
COPY . .

# Compiler Angular en mode production
RUN npm run build -- --configuration production

# ===== Stage 2: Servir avec Nginx =====
FROM nginx:alpine

# Copier la configuration nginx personnalisée
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf /etc/nginx/conf.d/default.conf

# Copier les fichiers compilés depuis le stage de build
COPY --from=builder /app/dist/demo/browser /usr/share/nginx/html

# Exposer le port 80
EXPOSE 80

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

# Démarrer Nginx
CMD ["nginx", "-g", "daemon off;"]
