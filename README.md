# FridaAI - Application Full-Stack

Application de gestion des fiches de succession (**Frida**) avec Angular + Spring Boot + PostgreSQL + OCR (EasyTess) + Calculs des parts, orchestrée avec Docker.

## 📋 Architecture

```
frida-ai/
├── backend/                 # Spring Boot Application
│   ├── Dockerfile          # Multi-stage build
│   ├── pom.xml
│   └── src/
├── frontend/                # Angular Application
│   ├── Dockerfile          # Multi-stage build
│   ├── nginx.conf
│   ├── default.conf
│   └── src/
├── docker-compose.yml       # Orchestration des services
├── .env                     # Variables d'environnement
├── .dockerignore            # Fichiers à ignorer dans Docker
├── Makefile                 # Commandes utiles
└── init-db.sql             # Initialisation PostgreSQL
```

## 🚀 Démarrage Rapide

### Prérequis

- Docker & Docker Compose (v2.0+)
- make (optionnel, pour les commandes raccourcies)

### Installation

1. **Cloner et configurer**
```bash
# Modifier le fichier .env si nécessaire
nano .env
```

2. **Construire et démarrer**
```bash
# Avec Docker Compose
docker-compose up -d --build

# Ou avec make
make up
```

3. **Accéder aux services**
- 🌐 **Frontend**: http://localhost:4200
- 🔌 **Backend API**: http://localhost:8080
- 📚 **Swagger UI**: http://localhost:8080/swagger-ui.html
- 🗄️ **PostgreSQL**: localhost:5432

## 📖 Commandes Principales

### Avec Docker Compose

```bash
# Démarrer les services
docker-compose up -d

# Arrêter les services
docker-compose down

# Voir les logs
docker-compose logs -f
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres

# Accès au shell
docker-compose exec backend bash
docker-compose exec postgres psql -U postgres -d fridaaidb
docker-compose exec frontend sh

# Redémarrer les services
docker-compose restart
```

### Avec Make (plus simple)

```bash
make help          # Afficher toutes les commandes
make build         # Construire les images
make up            # Démarrer
make down          # Arrêter
make logs          # Voir les logs
make logs-backend  # Logs du backend
make clean         # Nettoyer tout
make ps            # État des containers
make shell-db      # Accès PostgreSQL
```

## 🌍 Variables d'Environnement (.env)

```env
# Application
APP_NAME=fridaocr
APP_VERSION=1.0.0

# Ports
BACKEND_PORT=8080
FRONTEND_PORT=4200
DATABASE_PORT=5432

# PostgreSQL
DB_VENDOR=postgresql
DB_HOST=postgres
DB_PORT=5432
DB_NAME=fridaaidb
DB_USER=postgres
DB_PASSWORD=Mmk!030809?

# Spring Boot
SPRING_PROFILES_ACTIVE=docker
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false

# CORS
CORS_ORIGINS=http://localhost:4200,http://localhost:3000

# Fichiers
FILE_UPLOAD_MAX_SIZE=10485760
```

## 🐳 Structure Docker

### Services

| Service | Port | Rôle |
|---------|------|------|
| **frontend** | 4200 | Angular + Nginx |
| **backend** | 8080 | Spring Boot API (orchestration) |
| **db** | 5432 | PostgreSQL Database |
| **calculs-api** | 8081 | Microservice de calcul des parts successorales (Spring Boot) |
| **ocr-api** | 8082 | Microservice OCR / Extraction de texte (Python Flask / EasyTess) |

### Network & Volumes

- **Network**: `frida-network` (communication inter-containers)
- **Volume**: `postgres_data` (persistance des données DB)
- **Volume**: `./backend/uploads` (stockage des fichiers)

## 📁 Backend (Spring Boot)

### Démarrage Local (sans Docker)

```bash
cd backend

# Build
mvn clean package

# Run
mvn spring-boot:run
```

### Configuration PostgreSQL

Assurez-vous que les variables d'environnement sont définis ou modifiez `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fridaaidb
spring.datasource.username=postgres
spring.datasource.password=Mmk!030809?
```

### Endpoints Principaux

- `GET /api/frida` - Liste des fiches
- `GET /api/frida/{numFrida}` - Détails d'une fiche
- `GET /api/frida/listeHeritiers/{numFrida}` - Héritiers d'une fiche
- `GET /api/frida/listeTemoins/{numFrida}` - Témoins d'une fiche
- `POST /api/files/upload` - Upload de fichiers
- `POST /api/folders/create` - Créer un dossier et lancer le traitement OCR

### Convention de nommage des dossiers d'upload

Chaque fichier est placé dans un sous-dossier dont le nom définit la catégorie de personne et le type de document :

| Dossier | Catégorie | Type de document |
|---------|-----------|------------------|
| `1_en`  | Défunt    | Extrait de naissance |
| `2_cni` | Conjoint  | Carte d'identité |
| `3_en`  | Enfant    | Extrait de naissance |
| `4_en`  | Parent    | Extrait de naissance |
| `5_en`  | Fratrie   | Extrait de naissance |
| `11_pp` | Témoin    | Passeport |

Format : `{code_catégorie}_{type_document}` (ex: `3_cni` = enfant avec CNI).

### Entités OCR

Les définitions d'entités OCR (zones + cadre de référence) sont stockées dans `easytess_ocr_api/entities/` :
- `en01.json` → Extrait de naissance
- `cni01.json` → Carte nationale d'identité
- `pp01.json` → Passeport

### Modèle de données (Entités principales)

```
FridaEntity
  ├── 1:1 → DefuntEntity → 1:1 → IdentitesEntity
  ├── 1:N → HeritierEntity → 1:1 → IdentitesEntity
  ├── 1:N → TemoinEntity → 1:1 → IdentitesEntity
  └── 1:1 → CalculEntity
```

> **Note :** `IdentitesEntity` est la table unifiée qui remplace les anciennes tables `ExtraitNaissanceEntity` et `PieceIdentiteEntity`. Elle stocke toutes les informations d'identité quel que soit le type de document (extrait, CNI, passeport).

## 🎨 Frontend (Angular)

### Démarrage Local (sans Docker)

```bash
cd frontend

# Installation
npm install

# Développement
npm start
# Accessible sur http://localhost:4200

# Build production
npm run build
```

### Structure

```
src/
├── app/
│   ├── components/
│   │   ├── accueil/      (Home, About)
│   │   ├── dossier/      (File Upload)
│   │   ├── frida/        (Fiche détail)
│   │   ├── frida-list/   (Liste)
│   │   ├── search/       (Recherche)
│   │   └── admin/        (Admin)
│   ├── services/         (HTTP, Business logic)
│   └── shared/           (Utils, interfaces)
└── styles/               (Global CSS)
```

## 🗄️ Base de Données

### Initialisation

La base de données est créée automatiquement au premier démarrage via `init-db.sql`.

### Accès Direct

```bash
# Via Docker
docker-compose exec postgres psql -U postgres -d fridaaidb

# Commandes utiles
\dt                 # Lister les tables
\d table_name       # Décrire une table
\du                 # Lister les utilisateurs
```

### Backup & Restore

```bash
# Backup
docker-compose exec postgres pg_dump -U postgres fridaaidb > backup.sql

# Restore
docker-compose exec -T postgres psql -U postgres fridaaidb < backup.sql

# Ou avec make
make db-backup
```

## 🔧 Configuration avancée

### Profiles Spring Boot

- **docker**: Configuration pour environment Docker (défaut)
- **development**: Configuration locale
- **production**: Configuration production

Modifier avec `SPRING_PROFILES_ACTIVE` dans `.env`

### Nginx Proxy

Le frontend (Nginx) proxie les requêtes `/api/*` vers le backend:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    # Headers et configuration...
}
```

## 🧪 Tests

```bash
# Tests Backend (Maven)
mvn test

# Tests Frontend (Angular)
npm test

# Ou avec make
make test
make test-frontend
```

## 🛡️ Sécurité

### Points importants

1. **Secrets**: Ne pas commiter le `.env` en production
2. **CORS**: Configurable via `CORS_ORIGINS`
3. **SSL/TLS**: À ajouter en production (Nginx/Load Balancer)
4. **Logs**: Logs activés en dev, à désactiver en prod
5. **Fichiers sensibles**: Protégés par .dockerignore

### À faire avant production

- [ ] Modifier les credentials par défaut
- [ ] Configurer SSL/TLS
- [ ] Mettre en place un WAF
- [ ] Configurer les logs centralisés
- [ ] Limiter les CORS
- [ ] Audit de sécurité des dépendances

## 🐛 Dépannage

### Backend ne démarre pas

```bash
# Vérifier les logs
make logs-backend

# Vérifier la connexion PostgreSQL
docker-compose exec postgres pg_isready
```

### PostgreSQL n'accepte pas les connexions

```bash
# Vérifier l'état du container
docker-compose ps postgres

# Redémarrer
docker-compose restart postgres

# Voir les logs
make logs-db
```

### Frontend n'accède pas au backend

1. Vérifier que le backend est accessible: `curl http://localhost:8080/api/frida`
2. Vérifier les CORS dans `.env`
3. Vérifier la config Nginx dans `frontend/default.conf`

## 📚 Ressources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Angular Documentation](https://angular.io)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)

## 📝 Logs & Monitoring

### Logs des containers

```bash
# Tous les services
docker-compose logs -f --tail=100

# Format structuré
docker-compose logs --timestamps

# Logs JSON
docker-compose logs --format json
```

### Monitoring (optionnel)

Pour ajouter Prometheus/Grafana:

1. Configurer Spring Boot Actuator (déjà inclus)
2. Ajouter les services dans `docker-compose.yml`

## 🚢 Déploiement

### Production Checklist

- [ ] Utiliser un registre Docker privé
- [ ] Configurer un système de secrets (Vault, K8s secrets)
- [ ] Ajouter SSL/TLS
- [ ] Configurer un reverse proxy (Nginx/HAProxy)
- [ ] Mettre en place les health checks
- [ ] Logs centralisés (ELK, Datadog, etc.)
- [ ] Monitoring et alerting

### Exemple Kubernetes

Structure pour K8s (non incluse):
```
k8s/
├── namespace.yaml
├── postgres.yaml
├── backend.yaml
├── frontend.yaml
└── ingress.yaml
```

## 📞 Support

Pour toute question ou problème, vérifiez:
1. Les logs (`make logs`)
2. Le status des containers (`make ps`)
3. Les variables d'environnement (`.env`)

## 📄 License

[Votre License]
# easytess_ocr_api
#   f r i d a - o c r - m i c r o s 
 
 