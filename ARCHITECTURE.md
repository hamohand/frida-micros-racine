# 🏗️ Architecture et Structure du Projet

## Vue d'ensemble

FridaAI est une application full-stack moderne utilisant:
- **Frontend**: Angular 18 avec TypeScript
- **Backend**: Spring Boot 3.3.4 avec Java 21
- **Base de données**: PostgreSQL 16
- **Orchestration**: Docker Compose

## 📂 Structure des dossiers

```
frida-ai/
├── backend/                          # Application Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/muhend/backendai/
│   │   │   │   ├── BackendAiApplication.java
│   │   │   │   ├── config/           # Configuration Spring
│   │   │   │   ├── controller/       # Endpoints REST
│   │   │   │   │   ├── FridaController.java
│   │   │   │   │   ├── FileController.java
│   │   │   │   │   └── FolderController.java
│   │   │   │   ├── dto/              # Data Transfer Objects
│   │   │   │   ├── entities/         # JPA Entities
│   │   │   │   │   ├── FridaEntity.java
│   │   │   │   │   ├── HeritierEntity.java
│   │   │   │   │   └── TemoinEntity.java
│   │   │   │   ├── repository/       # JPA Repositories
│   │   │   │   └── service/          # Business Logic
│   │   │   │       ├── FridaService.java
│   │   │   │       └── FileUploadService.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── templates/
│   │   │       └── static/
│   │   └── test/                     # Tests unitaires
│   ├── pom.xml                       # Dépendances Maven
│   ├── Dockerfile                    # Build image Docker backend
│   └── uploads/                      # Fichiers uploadés
│
├── frontend/                         # Application Angular
│   ├── src/
│   │   ├── app/
│   │   │   ├── app.component.*       # Root component
│   │   │   ├── app.config.ts         # Configuration app
│   │   │   ├── app.routes.ts         # Routing
│   │   │   ├── components/
│   │   │   │   ├── accueil/          # Pages d'accueil
│   │   │   │   │   ├── home/
│   │   │   │   │   └── about/
│   │   │   │   ├── admin/            # Interface admin
│   │   │   │   ├── dossier/          # Gestion fichiers
│   │   │   │   │   ├── create-person/
│   │   │   │   │   ├── file-upload/
│   │   │   │   │   └── upload-windows/
│   │   │   │   ├── frida/            # Détail fiche
│   │   │   │   ├── frida-list/       # Liste fiches
│   │   │   │   └── search/           # Recherche
│   │   │   ├── services/             # Services HTTP & Business
│   │   │   │   ├── frida.service.ts
│   │   │   │   ├── file-upload.service.ts
│   │   │   │   ├── folder.service.ts
│   │   │   │   ├── lireai-ecrirebd.service.ts
│   │   │   │   └── traduction-arabe.service.ts
│   │   │   └── shared/               # Utilitaires partagés
│   │   │       └── utils/
│   │   │           └── file.utils.ts
│   │   ├── styles/                   # Styles CSS globaux
│   │   │   ├── buttons.css
│   │   │   ├── layout.css
│   │   │   ├── navigation.css
│   │   │   └── variables.css
│   │   ├── main.ts                   # Entry point
│   │   └── index.html                # HTML template
│   ├── package.json
│   ├── tsconfig.json
│   ├── angular.json
│   ├── Dockerfile                    # Build image Docker frontend
│   ├── nginx.conf                    # Configuration Nginx
│   ├── default.conf                  # Configuration site Nginx
│   └── dist/                         # Build production
│
├── docker-compose.yml                # Orchestration services
├── .env                              # Variables d'environnement
├── .env.example                      # Exemple variables
├── .dockerignore                     # Fichiers à ignorer Docker
├── .gitignore                        # Fichiers à ignorer Git
├── Makefile                          # Commandes raccourcies
├── README.md                         # Documentation principale
├── ARCHITECTURE.md                   # Ce fichier
├── start.sh                          # Script démarrage
├── stop.sh                           # Script arrêt
├── init-db.sql                       # Initialisation PostgreSQL
├── pom.xml                           # Racine (legacy, à archiver)
├── package.json                      # Racine (legacy, à archiver)
└── compose.yaml                      # Ancien (à supprimer)
```

## 🔄 Flux de communication

```
┌─────────────────┐
│   Navigateur    │
└────────┬────────┘
         │ HTTP/HTTPS:4200
         ▼
    ┌────────────────────────────┐
    │  Frontend (Angular + Nginx)│
    │  - SPA Routing             │
    │  - Proxy /api/* → Backend  │
    └────────────────┬───────────┘
                     │ HTTP:8080
                     ▼
         ┌───────────────────────┐
         │ Backend (Spring Boot) │
         │ - REST API Endpoints  │
         │ - Business Logic      │
         └───────────┬───────────┘
                     │ JDBC:5432
                     ▼
         ┌───────────────────────┐
         │ PostgreSQL Database   │
         │ - Données persistantes│
         └───────────────────────┘
```

## 🔐 Sécurité - Couches

```
Client Browser
      ↓
   [CORS] ← Frontend filtre les origines
      ↓
 Nginx Proxy  ← Cache, compression, headers de sécurité
      ↓
[Spring Security] ← Authentification, autorisation
      ↓
 Business Logic ← Validation, autorité métier
      ↓
 PostgreSQL ← Cryptage au repos, contrôle d'accès
```

## 📊 Schéma de la Base de Données

### Entités principales

**FridaEntity** (Fiches de succession)
```sql
├── numFrida (PK)
├── dateDeclaration
├── dateExecution
├── defunt (FK → Defunt)
├── heritiers (OneToMany → HeritierEntity)
└── temoins (OneToMany → TemoinEntity)
```

**HeritierEntity**
```sql
├── id (PK)
├── frida (FK → FridaEntity)
├── nom
├── prenom
├── pourcentage
└── dateAjout
```

**TemoinEntity**
```sql
├── id (PK)
├── frida (FK → FridaEntity)
├── nom
├── prenom
└── dateAjout
```

## 🚀 Processus de Build & Déploiement

### Frontend
```
src/ → Angular Compiler → dist/
       ↓
     Nginx Alpine Image
       ↓
     Container Registry
```

### Backend
```
src/ → Maven Build → target/app.jar
       ↓
     JRE 21 Alpine Image
       ↓
     Container Registry
```

## 🔗 Dépendances clés

### Backend (Java 21, Maven)
- **spring-boot-starter-web**: Framework web
- **spring-boot-starter-data-jpa**: ORM
- **postgresql**: Driver JDBC
- **lombok**: Boilerplate reduction
- **springdoc-openapi**: Swagger/OpenAPI
- **google-cloud-document-ai**: Document processing
- **spring-boot-starter-actuator**: Health checks

### Frontend (Node 20, Angular 18)
- **@angular/core**: Framework principal
- **@angular/material**: Composants UI
- **@angular/forms**: Gestion des formulaires
- **rxjs**: Programmation réactive
- **typescript**: Typage fort

## ♻️ Patterns et Principes

### Patterns utilisés
- **MVC** (Spring Boot)
- **Component-Based Architecture** (Angular)
- **Service Layer** (Séparation des responsabilités)
- **Repository Pattern** (Data Access)
- **Singleton** (Spring Beans)
- **Observer** (RxJS)

### Principes appliqués
- **SOLID**: Single Responsibility, Open/Closed
- **DRY**: Don't Repeat Yourself
- **Clean Code**: Lisibilité et maintenabilité
- **Docker Best Practices**: Multi-stage builds, Alpine images

## 🔄 Intégration Continue (CI/CD) - À implémenter

Exemple pour GitHub Actions:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Backend Tests
        run: mvn test
      - name: Frontend Tests
        run: npm test
      
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Images
        run: docker-compose build
      - name: Push to Registry
        run: docker-compose push
```

## 📈 Scalabilité

Pour scalabiliser l'application:

1. **Database**: Réplication PostgreSQL, Connection Pooling
2. **Backend**: Kubernetes, Load Balancer, Cache distributed (Redis)
3. **Frontend**: CDN pour les assets, Service Worker pour offline
4. **Monitoring**: Prometheus, Grafana, ELK Stack

## 🛠️ Développement local

### Configuration IDE

**VS Code** (Recommandé)
```json
{
  "extensions": [
    "ms-vscode.extension-pack-for-java",
    "Angular.ng-template",
    "esbenp.prettier-vscode",
    "ms-azuretools.vscode-docker"
  ]
}
```

**IntelliJ IDEA**
- Plugins: Spring Boot, Angular, Docker

### Debug

**Backend**
```bash
mvn -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" spring-boot:run
```

**Frontend**
```bash
ng serve --disable-host-check
# Puis ouvrir Chrome DevTools
```

## 📚 Documentation supplémentaire

- [CONTRIBUTING.md](CONTRIBUTING.md) - Guide de contribution
- [DEPLOYMENT.md](DEPLOYMENT.md) - Guide de déploiement
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Dépannage
- [API_DOCUMENTATION.md](API_DOCUMENTATION.md) - API détaillée

---

**Dernière mise à jour**: Janvier 2026
**Version**: 1.0.0
