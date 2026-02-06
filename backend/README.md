# Backend - FridaAI

Application Spring Boot pour l'API REST de gestion des fiches de succession.

## 📋 Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/muhend/backendai/
│   │   │       ├── BackendAiApplication.java
│   │   │       ├── config/              # Configuration Spring
│   │   │       ├── controller/          # Endpoints REST
│   │   │       ├── dto/                 # Data Transfer Objects
│   │   │       ├── entities/            # JPA Entities
│   │   │       ├── repository/          # Data Access
│   │   │       └── service/             # Business Logic
│   │   └── resources/
│   │       ├── application.properties   # Configuration
│   │       ├── templates/
│   │       └── static/
│   └── test/
│       └── java/                        # Tests unitaires
├── .mvn/
│   └── wrapper/
├── pom.xml                              # Dépendances Maven
├── Dockerfile                           # Build Docker
└── uploads/                             # Fichiers uploadés

```

## 🚀 Démarrage

### Local (sans Docker)

```bash
# Compiler
mvn clean compile

# Démarrer
mvn spring-boot:run

# Tests
mvn test
```

### Docker

```bash
# Build
docker build -t frida-ai-backend .

# Run
docker run -d -p 8080:8080 \
  -e DB_HOST=postgres \
  -e DB_NAME=fridaaidb \
  -e DB_USER=postgres \
  -e DB_PASSWORD=password \
  frida-ai-backend
```

### Docker Compose

```bash
# Depuis la racine du projet
docker-compose up backend
```

## 🔌 Endpoints Principaux

- `GET /api/frida` - Liste des fiches
- `GET /api/frida/{numFrida}` - Détails d'une fiche
- `POST /api/files/upload` - Upload de fichiers
- `GET /swagger-ui.html` - Documentation API

## 📊 Configuration

Voir `src/main/resources/application.properties`

Variables d'environnement:
- `DB_HOST` - Host PostgreSQL
- `DB_PORT` - Port PostgreSQL
- `DB_NAME` - Nom base de données
- `DB_USER` - Utilisateur BD
- `DB_PASSWORD` - Password BD

## 🧪 Tests

```bash
mvn test
mvn test -Dtest=FridaControllerTest
```

## 🐳 Build Docker

```bash
# Build local
docker build -t frida-ai-backend:latest .

# Build avec tag
docker build -t myregistry/frida-ai-backend:1.0.0 .

# Push
docker push myregistry/frida-ai-backend:1.0.0
```

## 📚 Documentation

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL JDBC](https://jdbc.postgresql.org/)

## 📝 Logs

```bash
# Voir les logs
mvn spring-boot:run -X

# Docker
docker logs frida-ai-backend
docker logs -f frida-ai-backend
```
