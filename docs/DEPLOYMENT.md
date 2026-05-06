# 📋 Guide de Déploiement - FridaAI

## ✅ Pré-déploiement Checklist

### Localement

- [ ] `.env` configuré avec les valeurs correctes
- [ ] Docker et Docker Compose installés
- [ ] `docker-compose up -d` exécuté avec succès
- [ ] Frontend accessible sur http://localhost:4200
- [ ] Backend API accessible sur http://localhost:8080
- [ ] Swagger UI fonctionnel sur http://localhost:8080/swagger-ui.html
- [ ] PostgreSQL accessible
- [ ] Tests passent (`make test`)

### Code

- [ ] Tous les secrets supprimés du code (vérifier Git history)
- [ ] `.env` ajouté à `.gitignore`
- [ ] README.md à jour
- [ ] Versions des dépendances vérifiées
- [ ] Pas de code commenté en production
- [ ] Logs activés pour les erreurs seulement

### Sécurité

- [ ] Credentials PostgreSQL changés
- [ ] CORS configuré correctement
- [ ] Headers de sécurité en place
- [ ] Dépendances vérifiées pour CVE (`mvn dependency-check:check`)
- [ ] SSL/TLS configuré
- [ ] Authentification/Autorisation implémentées

## 🚀 Déploiement en Environnement Intermédiaire (Staging)

### 1. Configuration

```bash
# Copier et adapter le .env pour staging
cp .env .env.staging

# Modifier .env.staging
nano .env.staging
```

Configuration recommandée:
```env
BACKEND_PORT=8080
FRONTEND_PORT=4200
DATABASE_PORT=5432
DB_PASSWORD=<strong-password>
CORS_ORIGINS=https://staging.frida.example.com
SPRING_JPA_SHOW_SQL=false
```

### 2. Build et Test

```bash
# Build avec les nouvelles versions
docker-compose --env-file .env.staging build --no-cache

# Tester localement
docker-compose --env-file .env.staging up -d

# Vérifier la santé
docker-compose ps
curl http://localhost:8080/actuator/health

# Tests
mvn test
npm test

# Arrêter
docker-compose --env-file .env.staging down
```

### 3. Push vers Registry

```bash
# Docker Hub ou autre registry
docker tag frida-ai-backend:latest myregistry.azurecr.io/frida-ai-backend:1.0.0
docker tag frida-ai-frontend:latest myregistry.azurecr.io/frida-ai-frontend:1.0.0

docker push myregistry.azurecr.io/frida-ai-backend:1.0.0
docker push myregistry.azurecr.io/frida-ai-frontend:1.0.0
```

### 4. Déploiement sur Serveur Intermédiaire

```bash
# SSH sur le serveur
ssh user@staging.example.com

# Cloner/mettre à jour le code
git clone https://github.com/yourorg/frida-ai.git
cd frida-ai

# Récupérer les images
docker pull myregistry.azurecr.io/frida-ai-backend:1.0.0
docker pull myregistry.azurecr.io/frida-ai-frontend:1.0.0

# Démarrer les services
docker-compose --env-file .env.staging up -d

# Vérifier
docker-compose logs -f
curl https://staging.frida.example.com
```

## 🌍 Déploiement en Production

### 1. Architecture recommandée

```
┌─────────────────────┐
│  Reverse Proxy      │
│  (Nginx/HAProxy)    │
│  + SSL/TLS          │
│  + Load Balancer    │
└──────────┬──────────┘
           │
    ┌──────┴──────┐
    │             │
┌───▼──┐      ┌──▼───┐
│App 1 │      │App 2 │  (Répliques pour HA)
└───┬──┘      └──┬───┘
    │            │
    └──────┬─────┘
           │
    ┌──────▼──────┐
    │ PostgreSQL  │
    │ Replication │  (Primary + Replicas)
    └─────────────┘
```

### 2. Configuration Production

```env
# .env.production

# ===== PORTS =====
BACKEND_PORT=8080
FRONTEND_PORT=80
DATABASE_PORT=5432

# ===== DATABASE =====
DB_HOST=postgres-primary.internal
DB_USER=postgres
DB_PASSWORD=<VERY_STRONG_PASSWORD>
DB_NAME=fridaaidb_prod

# ===== SPRING =====
SPRING_PROFILES_ACTIVE=production
SPRING_JPA_HIBERNATE_DDL_AUTO=validate  # Important: validate, jamais update
SPRING_JPA_SHOW_SQL=false
LOG_LEVEL=WARN

# ===== CORS =====
CORS_ORIGINS=https://frida.example.com

# ===== SECURITY =====
FILE_UPLOAD_MAX_SIZE=10485760

# ===== DOCKER =====
COMPOSE_PROJECT_NAME=frida-ai-prod
```

### 3. Étapes de déploiement

#### a) Préparation des données

```bash
# Backup de la BD de staging
docker-compose exec postgres pg_dump -U postgres fridaaidb > backup_pre_prod.sql

# Transfert sécurisé (SFTP/SCP)
scp backup_pre_prod.sql user@production.example.com:/data/
```

#### b) Infrastructure

```bash
# Créer les dossiers/volumes nécessaires
mkdir -p /data/postgres_data
mkdir -p /app/uploads
chmod 700 /data/postgres_data
```

#### c) Déploiement du code

```bash
# Sur le serveur production
cd /opt/frida-ai

# Récupérer la dernière version (avec tags Git)
git fetch origin
git checkout v1.0.0  # Tag de release

# Mettre à jour .env
nano .env.production
```

#### d) Migration BDD

```bash
# Restaurer les données
docker-compose exec -T postgres psql -U postgres fridaaidb < /data/backup_pre_prod.sql

# Vérifier l'intégrité
docker-compose exec postgres psql -U postgres -d fridaaidb -c "SELECT COUNT(*) FROM frida;"
```

#### e) Lancement

```bash
# Démarrer les services
docker-compose --env-file .env.production up -d

# Vérifier la santé
sleep 30
curl https://frida.example.com/api-docs
curl https://frida.example.com  # Frontend
```

#### f) Smoke Tests

```bash
# Tests de santé
curl -H "Accept: application/json" https://api.frida.example.com/actuator/health

# Vérifier une API
curl https://api.frida.example.com/api/frida

# Vérifier le frontend
curl https://frida.example.com | grep "<title>"
```

### 4. Monitoring & Logging

```bash
# Logs temps réel
docker-compose logs -f --tail=100

# Exporter les logs
docker-compose logs > logs_$(date +%Y%m%d_%H%M%S).txt

# Intégration ELK (Elasticsearch, Logstash, Kibana)
# À ajouter dans docker-compose.yml pour la production
```

## 🔄 Stratégie de Mise à Jour

### Blue-Green Deployment

```bash
# 1. Version actuelle (Blue)
docker-compose -f docker-compose.blue.yml up -d

# 2. Déployer la nouvelle version (Green)
docker-compose -f docker-compose.green.yml up -d

# 3. Tester Green
curl http://localhost:4201  # Port Green

# 4. Si OK, basculer le load balancer vers Green
# update nginx/haproxy config

# 5. Garder Blue en backup pour rollback rapide
```

### Rolling Updates (Kubernetes)

```yaml
# deployment.yaml pour K8s
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frida-ai-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    # ...
```

## 🔙 Rollback Plan

### Scénario 1: Rollback immédiat

```bash
# Si déploiement échoue
docker-compose --env-file .env.production down

# Redémarrer la version précédente
docker-compose --env-file .env.production up -d

# Ou avec tag de version
docker run -d \
  --env-file .env.production \
  myregistry/frida-ai-backend:1.0.0  # Version précédente
```

### Scénario 2: Rollback de BDD

```bash
# Si une migration de BD a échoué
docker-compose exec postgres pg_restore -U postgres -d fridaaidb < backup_pre_prod.sql

# Vérifier
docker-compose exec postgres psql -U postgres -d fridaaidb -c "\dt"

# Redémarrer backend
docker-compose restart backend
```

## 🛡️ Sécurité en Production

### Avant le déploiement

```bash
# 1. Vérifier les CVE
mvn org.owasp:dependency-check-maven:check

# 2. Scanner les images Docker
trivy image myregistry/frida-ai-backend:1.0.0
trivy image myregistry/frida-ai-frontend:1.0.0

# 3. Audit des secrets
git log -p | grep -i "password\|api_key\|secret"

# 4. Vérifier les permissions
ls -la /data/postgres_data
# Doit être: drwx------ (700)
```

### Configuration Nginx/Reverse Proxy

```nginx
# Security Headers
add_header X-Content-Type-Options "nosniff";
add_header X-Frame-Options "SAMEORIGIN";
add_header X-XSS-Protection "1; mode=block";
add_header Referrer-Policy "no-referrer";
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()";

# HTTPS redirect
server {
  listen 80;
  server_name frida.example.com;
  return 301 https://$server_name$request_uri;
}

# SSL Configuration
server {
  listen 443 ssl http2;
  server_name frida.example.com;
  
  ssl_certificate /etc/ssl/certs/frida.crt;
  ssl_certificate_key /etc/ssl/private/frida.key;
  ssl_protocols TLSv1.2 TLSv1.3;
  ssl_ciphers HIGH:!aNULL:!MD5;
  ssl_prefer_server_ciphers on;
}
```

## 📊 Monitoring Post-Déploiement

### Métriques à surveiller

- **Uptime**: Doit être > 99.9%
- **Response Time**: Doit être < 200ms
- **Error Rate**: Doit être < 0.1%
- **CPU Usage**: Idéalement < 70%
- **Memory Usage**: Idéalement < 80%
- **Disk Usage**: Idéalement < 70%

### Alertes recommandées

```yaml
# prometheus alerts (exemple)
- alert: HighErrorRate
  expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.001
  for: 5m
  
- alert: HighLatency
  expr: histogram_quantile(0.95, http_request_duration_seconds) > 0.5
  for: 5m
```

## 📝 Documenter le déploiement

```markdown
# Déploiement v1.0.0 - 2026-01-08

## Changements
- Migration MySQL → PostgreSQL
- Restructuration frontend/backend
- Support Docker complet

## Tests
- [x] Tests unitaires passés
- [x] Tests d'intégration passés
- [x] Tests de charge OK (1000 req/s)

## Rollout
- [x] Staging déployé avec succès
- [x] 1h de smoke tests
- [x] Bascule vers Production effectuée
- [x] Validation post-déploiement OK

## Incidents
Aucun

## Notes
Monitoring à 5 min en prod pendant 24h.
```

## 🚨 En cas de problème

```bash
# 1. Vérifier les logs
docker-compose logs backend | tail -50

# 2. Vérifier la connectivité BDD
docker-compose exec backend \
  java -cp /app/app.jar org.postgresql.ds.PGSimpleDataSource \
  jdbc:postgresql://postgres:5432/fridaaidb

# 3. Redémarrer gracieux
docker-compose restart backend

# 4. Contrôle d'intégrité
docker-compose exec postgres \
  psql -U postgres -d fridaaidb -c "ANALYZE;"

# 5. Contact de secours
# Escalade: SRE Team → DevOps → Architecture
```

---

**Dernière mise à jour**: Janvier 2026
**Version du guide**: 1.0
