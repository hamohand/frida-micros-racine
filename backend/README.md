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
│   │   │       ├── config/              # Configuration Spring (CORS, RestTemplate)
│   │   │       ├── controller/          # Endpoints REST
│   │   │       ├── dto/                 # Data Transfer Objects
│   │   │       │   └── DocumentInfo.java  # Parse noms de dossiers ({code}_{type})
│   │   │       ├── entities/            # JPA Entities
│   │   │       │   ├── FridaEntity.java
│   │   │       │   ├── DefuntEntity.java
│   │   │       │   ├── HeritierEntity.java
│   │   │       │   ├── TemoinEntity.java
│   │   │       │   ├── IdentitesEntity.java    # Table unifiée d'identités
│   │   │       │   └── CalculEntity.java
│   │   │       ├── enums/               # Enumerations
│   │   │       │   ├── DocumentType.java    # EXTRAIT_NAISSANCE, CNI, PASSEPORT
│   │   │       │   └── HeirCategory.java    # DEFUNT(1), CONJOINT(2), etc.
│   │   │       ├── repository/          # Data Access (JPA Repositories)
│   │   │       ├── client/              # Clients HTTP vers microservices
│   │   │       │   ├── ocr/             # Client OCR API
│   │   │       │   │   ├── OcrApiClient.java
│   │   │       │   │   └── dto/         # OcrAnalysisRequestDto, etc.
│   │   │       │   └── calculs/         # Client Calculs API
│   │   │       └── service/             # Business Logic
│   │   │           ├── aibd/
│   │   │           │   ├── EcrireBdService.java      # Orchestration OCR + BD
│   │   │           │   ├── LectureAiService.java     # Lecture dossiers
│   │   │           │   └── LectureExtraitAi.java     # Mapping Document AI
│   │   │           └── FileUploadService.java
│   │   └── resources/
│   │       ├── application.properties   # Configuration
│   │       └── application-docker.properties
│   └── test/
│       └── java/                        # Tests unitaires
├── pom.xml                              # Dépendances Maven
├── Dockerfile                           # Multi-stage build Docker
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
- `GET /api/frida/listeHeritiers/{numFrida}` - Héritiers d'une fiche
- `GET /api/frida/listeTemoins/{numFrida}` - Témoins d'une fiche
- `POST /api/files/upload` - Upload de fichiers
- `POST /api/folders/create` - Créer un dossier et lancer le traitement OCR
- `GET /swagger-ui.html` - Documentation API

## 📄 Modèle de données

### IdentitesEntity (table unifiée)

Remplace les anciennes tables `ExtraitNaissanceEntity` et `PieceIdentiteEntity`.
Stocke toutes les informations d'identité quel que soit le type de document :

| Champ | Description |
|-------|-------------|
| nom, prenom | Nom et prénom |
| latines, prenomLatines | En caractères latins |
| dateNaissance | Date de naissance |
| sexe | Sexe |
| pere, mere | Nom du père et de la mère |
| numeroPiece | Numéro du document |
| delivrePar, delivreLe, expireLe | Info pièce d'identité |

### Relations

```
FridaEntity
  ├── 1:1 → DefuntEntity → 1:1 → IdentitesEntity
  ├── 1:N → HeritierEntity → 1:1 → IdentitesEntity
  ├── 1:N → TemoinEntity → 1:1 → IdentitesEntity
  └── 1:1 → CalculEntity
```

## 🔗 Microservices appelés

| Service | URL | Rôle |
|---------|-----|------|
| **OCR API** | `http://host.docker.internal:8082` | Extraction de texte (Tesseract + EasyOCR) |
| **Calculs API** | `http://calculs-api:8081` | Calcul parts successorales |

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
